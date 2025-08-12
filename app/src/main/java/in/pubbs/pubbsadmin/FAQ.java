package in.pubbs.pubbsadmin;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import in.pubbs.pubbsadmin.Adapter.FAQAdapter;
import in.pubbs.pubbsadmin.Model.FAQList;

public class FAQ extends AppCompatActivity {
    private RecyclerView recyclerView;
    private RecyclerView.LayoutManager layoutManager;
    String TAG = FAQ.class.getSimpleName();
    private List<FAQList> questionList = new ArrayList<>();
    private FAQAdapter faqAdapter;
    LayoutInflater inflater;
    TextView title;
    ImageView back;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_faq);
        initView();
    }

    private void initView() {
        title = findViewById(R.id.toolbar_title);
        title.setText("FAQs");
        back = findViewById(R.id.back_button);
        recyclerView = findViewById(R.id.recycler_view);
        faqAdapter = new FAQAdapter(questionList, Objects.requireNonNull(getSupportFragmentManager()));
        layoutManager = new LinearLayoutManager(getApplicationContext());
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(faqAdapter);
        prepareFAQ();
        back.setOnClickListener(v -> {
            Intent intent = new Intent(FAQ.this, Profile.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(FAQ.this, Profile.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private void prepareFAQ() {
        FAQList faqList = new FAQList(getResources().getString(R.string.login_faq_one), getResources().getString(R.string.login_faq_one_ans));
        questionList.add(faqList);
        faqList = new FAQList(getResources().getString(R.string.login_faq_two), getResources().getString(R.string.login_faq_two_ans));
        questionList.add(faqList);
        faqList = new FAQList(getResources().getString(R.string.faq_three), getResources().getString(R.string.faq_three_ans));
        questionList.add(faqList);
        faqList = new FAQList(getResources().getString(R.string.faq_four), getResources().getString(R.string.faq_four_ans));
        questionList.add(faqList);
        faqList = new FAQList(getResources().getString(R.string.faq_five), getResources().getString(R.string.faq_five_ans));
        questionList.add(faqList);
        faqList = new FAQList(getResources().getString(R.string.faq_six), getResources().getString(R.string.faq_six_ans));
        questionList.add(faqList);
        faqList = new FAQList(getResources().getString(R.string.faq_seven), getResources().getString(R.string.faq_seven_ans));
        questionList.add(faqList);
        faqList = new FAQList(getResources().getString(R.string.faq_eight), getResources().getString(R.string.faq_eight_ans));
        questionList.add(faqList);
        faqList = new FAQList(getResources().getString(R.string.faq_nine), getResources().getString(R.string.faq_nine_ans));
        questionList.add(faqList);
        faqAdapter.notifyDataSetChanged();
    }
}
