package com.sarancha1337.mindreader;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.SystemClock;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;
import com.jjoe64.graphview.DefaultLabelFormatter;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class WorkActivity extends AppCompatActivity {

    private static boolean isFile;
    private static boolean isDeviceChoosed;
    private static boolean isBoth;
    private static long videoDuration;
    private static ArrayList<String> emotions;
    private static DateFormat formatter = new SimpleDateFormat("mm:ss.SSS", Locale.US);
    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    private SectionsPagerAdapter mSectionsPagerAdapter;
    private Uri videoUri;
    private GraphicsFragment fr1;
    private GraphicsFragment fr2;
    private VideoFragment vf;
    private CameraFragment cf;

    private TextView emtnText;

    private boolean isExit;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private ViewPager mViewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_work);

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mViewPager.setOffscreenPageLimit(3);

        formatter.setTimeZone(TimeZone.getTimeZone("UTC"));

        emotions = new ArrayList<>();

        isExit = false;

        emtnText = findViewById(R.id.emotionsText);

        ArrayList<ArrayList<DataPoint>> data = new ArrayList<>();

        try {
            Bundle extras = getIntent().getExtras();
            isFile = extras.getBoolean("isFile");
            if (isFile)
                data = readFile(extras.getString("path"));
            else {
                videoUri = Uri.parse(extras.getString("video"));
                vf = VideoFragment.newInstance(videoUri);
                videoDuration = extras.getLong("duration");
                isDeviceChoosed = extras.getBoolean("isDeviceChoosed");
                isBoth = extras.getBoolean("isBoth");
                if (isDeviceChoosed || isBoth) {
                    BluetoothSocketHandler.beginListenForData();
                    if (BluetoothSocketHandler.getCurrentMessage().equals("error\n"))
                        finish();
                }
            }
        } catch (NullPointerException e) {
            finish();
        }

        final MyTimer timer = new MyTimer(videoDuration, 100);

        if (isFile)
            timer.setData(data);

        fr1 = GraphicsFragment.newInstance(1, timer);
        fr2 = GraphicsFragment.newInstance(2, timer);

        final Button startB = findViewById(R.id.startB);
        final Button stopB = findViewById(R.id.stopB);
        final Button resetB = findViewById(R.id.resetB);

        View.OnClickListener start = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                timer.start();
                vf.start();
                cf.start(WorkActivity.this);
                startB.setEnabled(false);
                stopB.setEnabled(true);
            }
        };
        startB.setOnClickListener(start);


        View.OnClickListener stop = new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (!isExit) {
                    stop(timer);
                    cf.stop(WorkActivity.this);
                } else
                    saveAndExit(timer);

                resetB.setEnabled(true);
            }
        };
        stopB.setOnClickListener(stop);


        final View.OnClickListener reset = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                reset(timer);
                resetB.setEnabled(false);
                startB.setEnabled(true);
                stopB.setEnabled(false);

            }
        };
        resetB.setOnClickListener(reset);

        cf = CameraFragment.newInstance();
    }

    @Override
    public void onBackPressed() {
        if (isDeviceChoosed || isBoth)
            BluetoothSocketHandler.Stop();
        if (cf != null)
            cf.stop(this);
        finish();
    }

    @Override
    protected void onDestroy() {
        if (cf != null)
            cf.stop(this);
        finish();
        super.onDestroy();
    }

    private ArrayList<ArrayList<DataPoint>> readFile(String filePath) {
        ArrayList<DataPoint> data1 = new ArrayList<>();
        ArrayList<DataPoint> data2 = new ArrayList<>();

        emtnText.setText(filePath);

        FileInputStream fstream = null;
        try {
            fstream = new FileInputStream(filePath);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
        String strLine;
        int i = 0;
        videoDuration = 1;

        while (true) {
            try {
                if ((strLine = br.readLine()) == null || strLine.equals("")) break;

                if (i == 0) {
                    videoUri = Uri.parse(strLine);
                    vf = VideoFragment.newInstance(videoUri);
                } else {
                    String[] line = strLine.split("\\|");
                    Date d = formatter.parse(line[2]);
                    data1.add(new DataPoint(d, Integer.valueOf(line[0])));
                    data2.add(new DataPoint(d, Integer.valueOf(line[1])));
                    emotions.add(line[3]);

                    videoDuration = d.getTime();

                }
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this.getApplicationContext(), "Файл повреждён!", Toast.LENGTH_SHORT).show();
                finish();
            }
            i++;
        }

        videoDuration = (long) data1.get(data1.size() - 1).getX();
        ArrayList<ArrayList<DataPoint>> answ = new ArrayList<>(2);
        answ.add(data1);
        answ.add(data2);

        return answ;
    }

    private void stop(MyTimer timer) {
        vf.stop();
        timer.cancel();

        timer.seeAllData();

        fr1.setGraphScrollable();
        fr2.setGraphScrollable();

        Button stopB = findViewById(R.id.stopB);

        if (isFile)
            stopB.setText("Выйти");
        else
            stopB.setText("Сохранить и выйти");

        isExit = true;
    }

    private void saveAndExit(MyTimer timer) {
        finish();
        if (isFile) {
            timer.clearAllData();
        } else {
            if (isDeviceChoosed || isBoth)
                BluetoothSocketHandler.Stop();

            StringBuilder text = new StringBuilder(videoUri.toString() + "\n");

            ArrayList<DataPoint> data1 = timer.getData1();
            ArrayList<DataPoint> data2 = timer.getData2();

            for (int i = 0; i < data1.size(); ++i)
                text.append((long) data1.get(i).getY()).append("|").append((long) data2.get(i).getY()).append("|").append(formatter.format((long) data1.get(i).getX())).append("|").append(emotions.get(i)).append("\n");

            timer.clearAllData();

            Intent intent = new Intent(WorkActivity.this, SaveActivity.class);
            intent.putExtra("text", text.toString());
            startActivity(intent);
        }
    }

    private void reset(MyTimer timer) {
        timer.reset();
        vf.reset();

        fr1.setGraphStable();
        fr2.setGraphStable();

        Button startB = findViewById(R.id.startB);
        startB.setEnabled(true);

        Button stopB = findViewById(R.id.stopB);
        stopB.setText("Остановить");

        isExit = false;
    }

    public static class VideoFragment extends Fragment {

        private static Uri uri;
        VideoView videoView;

        public VideoFragment() {
        }

        static VideoFragment newInstance(Uri videoUri) {
            uri = videoUri;
            return new VideoFragment();
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

            View rootView = inflater.inflate(R.layout.work_video, container, false);
            videoView = rootView.findViewById(R.id.videoView);
            videoView.setVideoURI(uri);
            return rootView;
        }

        void stop() {
            videoView.pause();
        }

        void start() {
            videoView.start();
        }

        void reset() {
            videoView.pause();
            videoView.setVideoURI(uri);
        }
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class GraphicsFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private boolean isFirst;
        private MyTimer timer;
        private GraphView graph;

        public GraphicsFragment() {
        }

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        static GraphicsFragment newInstance(int sectionNumber, MyTimer t) {
            GraphicsFragment fragment = new GraphicsFragment();
            fragment.isFirst = sectionNumber == 1;
            fragment.timer = t;

            return fragment;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, final ViewGroup container, Bundle savedInstanceState) {
            final View rootView = inflater.inflate(R.layout.work_graph, container, false);

            final TextView textView = rootView.findViewById(R.id.graph1_text);

            graph = rootView.findViewById(R.id.graph);

            if (isFirst) {
                textView.setText("Сила эмоции:");
                graph.addSeries(timer.getSeries1());
                if (!isFile && isBoth)
                    graph.addSeries(timer.getSeries11());
            } else {
                textView.setText("Окраска эмоции:");
                graph.addSeries(timer.getSeries2());
                if (!isFile && isBoth)
                    graph.addSeries(timer.getSeries22());
            }

            setGraphStable();

            graph.getGridLabelRenderer().setLabelFormatter(new DefaultLabelFormatter() {
                @Override
                public String formatLabel(double value, boolean isValueX) {
                    if (isValueX) {
                        return formatter.format(new Date((long) value));
                    } else
                        return super.formatLabel(value, isValueX);
                }
            });

            return rootView;
        }

        void setGraphScrollable() {
            graph.getViewport().setScalableY(true);
            graph.getViewport().setScalable(true);
        }

        void setGraphStable() {
            graph.getViewport().setXAxisBoundsManual(true);
            graph.getViewport().setMinX(0);
            graph.getViewport().setMaxX(10000);

            graph.getViewport().setScalableY(false);
            graph.getViewport().setScalable(false);
        }
    }

    public static class CameraFragment extends Fragment {
        private static CameraHandler handler;

        public CameraFragment() {
        }

        static CameraFragment newInstance() {
            return new CameraFragment();
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

            View rootView = inflater.inflate(R.layout.work_camera, container, false);

            boolean detect = !isFile && (!isDeviceChoosed || isBoth);

            SurfaceView sv = rootView.findViewById(R.id.surfaceView);
            handler = new CameraHandler(getContext(), sv, detect);

            return rootView;
        }

        void start(Activity act) {
            if (!isFile)
                handler.startRecording(act);
        }

        void stop(Activity act) {
            if (!isFile)
                handler.stopRecording(act);
        }

        String getEmotion() {
            return handler.getEmotion();
        }

        float getValence() {
            return handler.getValence();
        }

        float getEng() {
            return handler.getEng();
        }

        void release() {
            handler.release();
        }
    }

    class MyTimer extends CountDownTimer {
        final static int maxx = 100;

        int nextData = 0;
        LineGraphSeries<DataPoint> series1;
        LineGraphSeries<DataPoint> series2;
        LineGraphSeries<DataPoint> series11;
        LineGraphSeries<DataPoint> series22;
        private ArrayList<DataPoint> data1;
        private ArrayList<DataPoint> data2;


        MyTimer(long duration, long interval) {
            super(duration, interval);

            data1 = new ArrayList<>();
            data2 = new ArrayList<>();

            series1 = new LineGraphSeries<>();
            series1.setColor(Color.RED);

            series2 = new LineGraphSeries<>();
            series2.setColor(Color.RED);

            if (!isFile) {
                series11 = new LineGraphSeries<>();
                series11.setColor(Color.GREEN);
                series22 = new LineGraphSeries<>();
                series22.setColor(Color.GREEN);
            }
        }

        @Override
        public void onTick(long millisUntilFinished) {

            DataPoint curStr;
            DataPoint curP;
            String curEm;


            if (isFile) {
                if (nextData < data1.size()) {
                    curStr = data1.get(nextData);
                    curP = data2.get(nextData);
                    curEm = emotions.get(nextData);

                    long x = (long)curStr.getX();
                    if(x > videoDuration - millisUntilFinished) {
                        SystemClock.sleep(x - (videoDuration - millisUntilFinished));
                    }
                } else {
                    return;
                }
            } else {
                Date date = new Date(videoDuration - millisUntilFinished);

                if (isBoth) {
                    String[] data = readFromEeg();
                    long x = Long.valueOf(data[0]);
                    long y = Long.valueOf(data[1]);
                    //
                    curStr = new DataPoint(date, x);
                    curP = new DataPoint(date, y);
                    curEm = data[2] + " по лицу: " + cf.getEmotion();


                    emotions.add(curEm);

                    data1.add(curStr);
                    data2.add(curP);


                    DataPoint faceStr = new DataPoint(date, cf.getEng());
                    DataPoint faceP = new DataPoint(date, cf.getValence());

                    series11.appendData(faceStr, true, maxx);
                    series22.appendData(faceP, true, maxx);
                } else if (isDeviceChoosed) {
                    String[] data = readFromEeg();
                    long x = Long.valueOf(data[0]);
                    long y = Long.valueOf(data[1]);
                    //
                    curStr = new DataPoint(date, x);
                    curP = new DataPoint(date, y);
                    curEm = data[2];


                    emotions.add(curEm);

                    data1.add(curStr);
                    data2.add(curP);
                } else{
                    curStr = new DataPoint(date, cf.getEng());
                    curP = new DataPoint(date, cf.getValence());
                    curEm = " по лицу: " + cf.getEmotion();


                    emotions.add(curEm);

                    data1.add(curStr);
                    data2.add(curP);
                }
            }
            nextData++;

            series1.appendData(curStr, true, maxx);
            series2.appendData(curP, true, maxx);


            if (emtnText != null)
                emtnText.setText("Текущая эмоция: " + curEm);

        }

        @Override
        public void onFinish() {
            stop(this);
        }

        LineGraphSeries<DataPoint> getSeries1() {
            return series1;
        }

        LineGraphSeries<DataPoint> getSeries2() {
            return series2;
        }

        LineGraphSeries<DataPoint> getSeries11() {
            return series11;
        }

        LineGraphSeries<DataPoint> getSeries22() {
            return series22;
        }

        ArrayList<DataPoint> getData1() {
            return data1;
        }

        ArrayList<DataPoint> getData2() {
            return data2;
        }

        void setData(ArrayList<ArrayList<DataPoint>> data) {
            data1 = data.get(0);
            data2 = data.get(1);
        }

        void clearAllData() {
            data1.clear();
            data2.clear();
            nextData = 0;
        }

        void reset() {
            cancel();
            if (!isFile) {
                data1.clear();
                data2.clear();
            }
            nextData = 0;

            DataPoint[] x = new DataPoint[1];
            x[0] = new DataPoint(-1, 0);
            series1.resetData(x);
            series2.resetData(x);
        }

        void seeAllData() {
            DataPoint[] d1 = new DataPoint[data1.size()];
            data1.toArray(d1);
            series1.resetData(d1);

            DataPoint[] d2 = new DataPoint[data2.size()];
            data2.toArray(d2);
            series2.resetData(d2);
        }

        String[] readFromEeg() {
            String msg = BluetoothSocketHandler.getCurrentMessage();
            if (msg.equals("error")) {
                stop(this);
                saveAndExit(this);
                Toast.makeText(WorkActivity.this.getApplicationContext(), "Произошла ошибка!", Toast.LENGTH_SHORT).show();
                //finish();
                return new String[]{"0", "0", "0"};
            }
            return msg.split("\\|");
        }
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case (0):
                    return vf;
                case (1):
                    return fr1;
                case (2):
                    return fr2;
                case (3):
                    return cf;
            }
            return null;
        }

        @Override
        public int getCount() {
            // Show 4 total pages.
//            if(!isFile)
//                return 4;
//            else
//                return 3;
            return 4;
        }
    }
}