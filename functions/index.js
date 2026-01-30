const {onSchedule} = require('firebase-functions/v2/scheduler');
const admin = require('firebase-admin');

admin.initializeApp();

const db = admin.database();

// -------- CONSTANTS (FIXED RULES) --------
const CHARGE_RATE_PER_MIN = 0.33;   // STATION: battery increases
const DISCHARGE_RATE_PER_MIN = -1.67; // RIDE: battery decreases
const IDLE_RATE_PER_MIN = 0;        // IDLE: no change

// Movement detection: if lat/lng changes more than this, treat as moved.
// ~0.00005 degrees ≈ ~5-6 meters (varies by latitude).
const MOVE_EPSILON_DEG = 0.00005;
const RIDE_STALE_MINUTES = 15;

function toNumberOrNull(v) {
  if (v == null) return null;
  const n = Number(v);
  return Number.isFinite(n) ? n : null;
}

/**
 * Helper: update battery for all cycles based on cycleState.
 * Reads: cyclebattery, cycleState, lastBatteryUpdatedAt from each cycle.
 * Updates: cyclebattery, lastBatteryUpdatedAt only.
 * No double drain, works even if function runs late.
 */
async function updateAllCycleBatteries() {
  const currentTime = Date.now();
  console.log('[BATTERY] Starting cycle battery update at', new Date(currentTime).toISOString());

  try {
    const rootRef = db.ref();
    const orgSnapshot = await rootRef.once('value');
    const orgs = orgSnapshot.val();

    if (!orgs) {
      console.log('[BATTERY] No organisations found at root');
      return { success: true, updated: 0 };
    }

    const orgNames = Object.keys(orgs);
    console.log('[BATTERY] Found organisations:', orgNames.join(', '));

    let updatedCount = 0;
    let skippedNoState = 0;
    let skippedNoElapsed = 0;
    let skippedUnknownState = 0;
    let skippedNotObject = 0;
    let movementUpdatedCount = 0;

    for (const orgName of orgNames) {
      const normalizedOrgName = orgName.replace(/\s/g, '');
      const bicycleRef = db.ref(`${normalizedOrgName}/Bicycle`);
      const bicycleSnapshot = await bicycleRef.once('value');

      if (!bicycleSnapshot.exists()) {
        console.log('[BATTERY] No Bicycle node for org:', normalizedOrgName);
        continue;
      }

      const bicycles = bicycleSnapshot.val();
      const cycleIds = Object.keys(bicycles);
      console.log('[BATTERY] Org', normalizedOrgName, 'has', cycleIds.length, 'cycles');

      for (const cycleId of cycleIds) {
        try {
          const cycle = bicycles[cycleId];
          if (!cycle || typeof cycle !== 'object') {
            skippedNotObject++;
            if (skippedNotObject <= 3) {
              console.log('[BATTERY] Skip (not object):', normalizedOrgName, '/', cycleId, 'type=', typeof cycle);
            }
            continue;
          }

          const oldBattery = cycle.cyclebattery;
          const updates = {};
          const state = cycle.cycleState;

          // -------- Movement tracking (for RIDE stale detection) --------
          // We maintain:
          // - lastLat/lastLng (baseline)
          // - lastMovementAt (updated when location changes)
          //
          // If a cycle is in RIDE and never had movement tracking, we set lastMovementAt=now
          // so it doesn't immediately look "stale".
          const lat = toNumberOrNull(cycle.latitude);
          const lng = toNumberOrNull(cycle.longitude);
          const lastLat = toNumberOrNull(cycle.lastLat);
          const lastLng = toNumberOrNull(cycle.lastLng);
          const lastMovementAt = toNumberOrNull(cycle.lastMovementAt);

          if (lat != null && lng != null) {
            if (lastLat == null || lastLng == null) {
              // initialize baseline
              updates.lastLat = lat;
              updates.lastLng = lng;
              if (state === 'RIDE' && lastMovementAt == null) {
                updates.lastMovementAt = currentTime;
              }
            } else {
              const moved =
                Math.abs(lat - lastLat) > MOVE_EPSILON_DEG ||
                Math.abs(lng - lastLng) > MOVE_EPSILON_DEG;
              if (moved) {
                updates.lastLat = lat;
                updates.lastLng = lng;
                updates.lastMovementAt = currentTime;
                movementUpdatedCount++;
              } else {
                if (state === 'RIDE' && lastMovementAt == null) {
                  updates.lastMovementAt = currentTime;
                }
              }
            }
          } else {
            // No location info; still ensure RIDE has a baseline lastMovementAt
            if (state === 'RIDE' && lastMovementAt == null) {
              updates.lastMovementAt = currentTime;
            }
          }

          let lastTs = cycle.lastBatteryUpdatedAt;

          if (state !== 'STATION' && state !== 'RIDE' && state !== 'IDLE') {
            skippedUnknownState++;
            if (skippedUnknownState <= 5) {
              console.log('[BATTERY] Skip (unknown state):', normalizedOrgName, '/', cycleId, 'cycleState=', JSON.stringify(state), 'cyclebattery=', oldBattery, 'lastBatteryUpdatedAt=', lastTs);
            }
            // Still apply any state/movement defaulting we collected
            if (Object.keys(updates).length > 0) {
              const cycleRef = db.ref(`${normalizedOrgName}/Bicycle/${cycleId}`);
              await cycleRef.update(updates);
            }
            continue;
          }

          const oldBatteryNum = (oldBattery != null && !isNaN(Number(oldBattery)))
            ? Number(oldBattery) : 100;
          if (lastTs == null || isNaN(Number(lastTs))) {
            lastTs = currentTime;
          } else {
            lastTs = Number(lastTs);
          }

          const elapsedMinutes = (currentTime - lastTs) / 60000;
          if (elapsedMinutes <= 0) {
            skippedNoElapsed++;
            if (skippedNoElapsed <= 3) {
              console.log('[BATTERY] Skip (elapsed<=0):', normalizedOrgName, '/', cycleId, 'elapsedMinutes=', elapsedMinutes.toFixed(4));
            }
            // Apply any non-battery updates (state defaulting / movement tracking)
            if (Object.keys(updates).length > 0) {
              const cycleRef = db.ref(`${normalizedOrgName}/Bicycle/${cycleId}`);
              await cycleRef.update(updates);
            }
            continue;
          }

          let rate;
          if (state === 'STATION') rate = CHARGE_RATE_PER_MIN;
          else if (state === 'RIDE') rate = DISCHARGE_RATE_PER_MIN;
          else rate = IDLE_RATE_PER_MIN;

          let newBattery = oldBatteryNum + (rate * elapsedMinutes);
          if (newBattery > 100) newBattery = 100;
          if (newBattery < 0) newBattery = 0;
          newBattery = Math.round(newBattery * 100) / 100;

          const cycleRef = db.ref(`${normalizedOrgName}/Bicycle/${cycleId}`);
          updates.cyclebattery = newBattery;
          updates.lastBatteryUpdatedAt = currentTime;
          await cycleRef.update(updates);

          updatedCount++;
          console.log('[BATTERY] Updated', normalizedOrgName, '/', cycleId, ':', oldBatteryNum + '% -> ' + newBattery + '%', 'state=' + state, 'elapsed=' + elapsedMinutes.toFixed(2) + 'min');
        } catch (err) {
          console.error('[BATTERY] Error for cycle', cycleId, ':', err.message);
        }
      }
    }

    console.log(
      '[BATTERY] Done.',
      'Updated=', updatedCount,
      'movementUpdated=', movementUpdatedCount,
      'skipped(unknownState)=', skippedUnknownState,
      'skipped(elapsed<=0)=', skippedNoElapsed,
      'skipped(notObject)=', skippedNotObject
    );
    return { success: true, updated: updatedCount };
  } catch (error) {
    console.error('[BATTERY] Fatal error:', error);
    return { success: false, error: error.message };
  }
}

/**
 * Scheduled: runs every 1 minute.
 * Reads from each cycle: cyclebattery, cycleState, lastBatteryUpdatedAt.
 * Rate: STATION +0.33/min, RIDE -1.67/min, IDLE 0.
 * Updates: cyclebattery, lastBatteryUpdatedAt only.
 */
exports.updateCycleBattery = onSchedule({
  schedule: 'every 1 minutes',
  timeZone: 'UTC',
  memory: '256MiB',
  timeoutSeconds: 540,
  invoker: 'public', // Allow Cloud Scheduler to invoke
}, async (event) => {
  console.log('Scheduled function triggered at:', new Date().toISOString());
  const result = await updateAllCycleBatteries();
  console.log('Scheduled function completed:', result);
  return result;
});

/**
 * HTTP Cloud Function to update cycle battery on demand
 * Can be called more frequently (e.g., every second via external cron)
 *
 * Usage: Call this HTTP endpoint to update all cycle batteries immediately
 * URL: https://[region]-[project-id].cloudfunctions.net/updateCycleBatteryHttp
 */
const {onRequest} = require('firebase-functions/v2/https');

exports.updateCycleBatteryHttp = onRequest({
  cors: true,
  memory: '256MiB',
  timeoutSeconds: 540,
}, async (req, res) => {
  try {
    const result = await updateAllCycleBatteries();
    res.status(200).json(result);
  } catch (error) {
    console.error('Error in updateCycleBatteryHttp:', error);
    res.status(500).json({ success: false, error: error.message });
  }
});
