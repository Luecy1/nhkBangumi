package com.github.luecy1.nhkbangumi.task;

import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.github.luecy1.nhkbangumi.Const;
import com.github.luecy1.nhkbangumi.Loading;
import com.github.luecy1.nhkbangumi.ProgramDetailActivity;
import com.github.luecy1.nhkbangumi.R;
import com.github.luecy1.nhkbangumi.entity.description.Description;
import com.github.luecy1.nhkbangumi.entity.description.DescriptionListRoot;
import com.github.luecy1.nhkbangumi.model.ProgramDetailModel;
import com.github.luecy1.nhkbangumi.service.NhkService;
import com.github.luecy1.nhkbangumi.util.CommonUtils;
import com.google.gson.Gson;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Call;
import retrofit2.Retrofit;

/**
 * Created by you on 2017/12/16.
 */

public class DetailAsyncTask extends AsyncTask<Void, Void, DescriptionListRoot> {

    private String url;
    private Context context;
    private ProgramDetailActivity activity;
    private Loading loading;

    public DetailAsyncTask(String url, Context context, ProgramDetailActivity activity, Loading loading) {
        this.url = url;
        this.context = context;
        this.activity = activity;
        this.loading = loading;
    }

    @Override
    protected DescriptionListRoot doInBackground(Void... params) {

        Request req = new Request
                .Builder()
                .url(url)
                .build();

        OkHttpClient client = new OkHttpClient();

        DescriptionListRoot descriptionListRoot = null;

        try {
            Response resp = client.newCall(req).execute();

            int resoCode = resp.code();
            if (resoCode != 200) {
                Toast.makeText(context, "サーバーエラー", Toast.LENGTH_LONG).show();
                return  null;
            }

            Gson gson = new Gson();
            descriptionListRoot = gson.fromJson(resp.body().string(),DescriptionListRoot.class);

            resp.body().close();

        } catch (IOException e) {
            Log.e("MyApp", "IOExceptionが発生しました。");
            e.printStackTrace();
        }


        return descriptionListRoot;
    }

    @Override
    protected void onPostExecute(DescriptionListRoot descriptionListRoot) {
        if (descriptionListRoot == null || descriptionListRoot.getList() == null) {
            return;
        }

        Description tmpDescription = null;
        if (descriptionListRoot.getList().getG1() != null) {
            tmpDescription = descriptionListRoot.getList().getG1().get(0);
        } else if (descriptionListRoot.getList().getG2() != null) {
            tmpDescription = descriptionListRoot.getList().getG2().get(0);
        } else if (descriptionListRoot.getList().getE1() != null) {
            tmpDescription = descriptionListRoot.getList().getE1().get(0);
        } else if (descriptionListRoot.getList().getE2() != null) {
            tmpDescription = descriptionListRoot.getList().getE2().get(0);
        } else if (descriptionListRoot.getList().getE3() != null) {
            tmpDescription = descriptionListRoot.getList().getE3().get(0);
        } else if (descriptionListRoot.getList().getE4() != null) {
            tmpDescription = descriptionListRoot.getList().getE4().get(0);
        } else if (descriptionListRoot.getList().getS1() != null) {
            tmpDescription = descriptionListRoot.getList().getS1().get(0);
        } else if (descriptionListRoot.getList().getS2() != null) {
            tmpDescription = descriptionListRoot.getList().getS2().get(0);
        } else if (descriptionListRoot.getList().getS3() != null) {
            tmpDescription = descriptionListRoot.getList().getS3().get(0);
        } else if (descriptionListRoot.getList().getS4() != null) {
            tmpDescription = descriptionListRoot.getList().getS4().get(0);
        }
        if (tmpDescription == null) {
            return;
        }
        final Description description = tmpDescription;

        // 番組ロゴ画像
        ImageView logo = activity.findViewById(R.id.program_detail_logo);
        if (description.getProgram_logo() != null) {
            Picasso.with(context).load("https:" + description.getProgram_logo().getUrl()).into(logo);
        } else {
        }

        ProgramDetailModel model = new ProgramDetailModel();
        model.setTitle(description.getTitle());
        model.setSubtitle(description.getSubtitle());
        model.setContent(description.getContent());
        model.setArea(description.getArea().getName());
        model.setService(description.getService().getName());
        // 日付変換
        String startDateString = "";
        String endDateString = "";
        try {
            Date startDate = CommonUtils.string2Date(description.getStart_time());
            Date endDate   = CommonUtils.string2Date(description.getEnd_time());
            SimpleDateFormat format = new SimpleDateFormat("yyyy年MM月dd日 ah:mm:ss", Locale.JAPAN);
            startDateString =  format.format(startDate).toString();
            endDateString   =  format.format(endDate).toString();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        model.setTime(startDateString + "～\n" + endDateString);
        model.setAct(description.getAct());

        StringBuilder genreStrBuilder = new StringBuilder();
        for (String genre : description.getGenres()) {
            if (genreStrBuilder.length() != 0) {
                genreStrBuilder.append(",");
            }
            genreStrBuilder.append(Const.GENRE_MAP_CODE.get(genre));
        }
        model.setGenre(genreStrBuilder.toString());

        model.setProgramUrl("https:" + description.getProgram_url());
        model.setEpisodeUrl("https:" + description.getProgram_url());

        if (description.getExtras() != null) {
            model.setExtras(
                      description.getExtras().getOndemand_program().getTitle()
                    + description.getExtras().getOndemand_episode().getTitle());
        } else {
            model.setExtras("その他の情報なし");
        }

//        ActivityProgramDetailBinding binding = DataBindingUtil.setContentView(activity, R.layout.activity_program_detail);
//        binding.setProgram(model);

        // Googleカレンダーに登録を有効化
        Button calenderButton = activity.findViewById(R.id.program_detail_calender);
        calenderButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Calendar startCal = Calendar.getInstance(Locale.JAPAN);
                        Calendar endCal   = Calendar.getInstance(Locale.JAPAN);
                        try {
                            Date startDate = CommonUtils.string2Date(description.getStart_time());
                            Date endDate   = CommonUtils.string2Date(description.getEnd_time());

                            startCal.setTime(startDate);
                            endCal.setTime(endDate);
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }

                        Intent intent = new Intent(Intent.ACTION_EDIT);
                        intent.setType("vnd.android.cursor.item/event");
                        intent.putExtra("title", description.getTitle());
                        intent.putExtra("description", description.getSubtitle());
                        intent.putExtra("beginTime", startCal.getTimeInMillis()); //開始日時
                        intent.putExtra("endTime", endCal.getTimeInMillis()); //終了日時
                        activity.startActivity(intent);
                    }
                }
        );

        loading.close();
    }
}
