package mc.com.project3;

import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.*;
import java.io.*;
import java.util.*;

public class MainActivity extends AppCompatActivity
{
    EditText nameEdit;
    TextView nameError;
    Button detectButton;
    Button predictButton;
    TextView result;
    TextView resultMinutes;
    TextView timeElapsed;
    GraphView heartRateGraph;

    Boolean isDetectPressed = false;
    int threshold = 60; // Threshold for bradycardia
    final int intervalLength = 5; // Interval length in minutes
    String name;
    BufferedReader bufferedReader;
    ArrayList<Integer> heartRate = new ArrayList<Integer>();
    ArrayList<Double> heartRateVariance = new ArrayList<>();
    ArrayList<Boolean> bradycardiaMinutes = new ArrayList<Boolean>();
    Boolean bradycardia_flag = false;


    double getMean(ArrayList<Double> input) {
        int size = input.size();
        double sum = 0.0;
        for (double a : input)
            sum += a;
        return sum / size;
    }

    double getVariance(ArrayList<Double> input) {
        int size = input.size();
        double mean = getMean(input);
        double temp = 0;
        for (double a : input)
            temp = temp + Math.pow(a-mean,2);
        return Math.sqrt(temp/size);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        nameEdit = findViewById(R.id.nameField);
        detectButton = findViewById(R.id.detectButton);
        predictButton = findViewById(R.id.predictButton);
        nameError = findViewById(R.id.nameError);
        result = findViewById(R.id.result);
        resultMinutes = findViewById(R.id.resultMinutes);
        timeElapsed = findViewById(R.id.timeElapsed);
        heartRateGraph = findViewById(R.id.heartRateGraph);

        heartRateGraph.setVisibility(View.INVISIBLE);
        result.setVisibility(View.INVISIBLE);
        resultMinutes.setVisibility(View.INVISIBLE);
        timeElapsed.setVisibility(View.INVISIBLE);

        nameError.setText("");

        detectButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view) {
                DetectTask dTask = new DetectTask();
                dTask.execute();
            }
        });

        predictButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PredictTask pTask = new PredictTask();
                pTask.execute();
            }
        });

    }


    private class DetectTask extends AsyncTask<Void, String, Void>
    {
        long startTime;
        long endtime;
        boolean fileError = false;
        ArrayList<Boolean> bradycardiaMinutes = new ArrayList<Boolean>();

        @Override
        protected void onPreExecute()
        {
            super.onPreExecute();
            nameError.setText("");
            heartRateGraph.removeAllSeries();

            result.setVisibility(View.VISIBLE);
            resultMinutes.setVisibility(View.VISIBLE);
            timeElapsed.setVisibility(View.VISIBLE);
            result.setText("");
            resultMinutes.setText("");
            timeElapsed.setText("");
            bradycardia_flag = false;
            heartRate =  new ArrayList<Integer>();
        }

        protected Void doInBackground(Void... params)
        {
            // Update flag
            isDetectPressed = true;
            name = nameEdit.getText().toString();
            System.out.println("Name: " + name);
            // Check if file name is not empty
            if(name.length() < 1)
            {
                fileError = true; // Update the error field
                return null; // Do no proceed
            }

            File sdcardPath = Environment.getExternalStorageDirectory();
            //System.out.println("SD Card Path"  + sdcardPath.toString());
            File filePath = new File(sdcardPath + "/Android/Data/CSE535_Project1_Group22/" + name + ".csv");
            //System.out.println("Full File Path"  + filePath.toString());
            if(!filePath.exists())
            {
                System.out.println("File dosn't exist" );
                fileError = true; // Update the error field
                return null; // Do no proceed
            }
            //start
            startTime = System.nanoTime();
            bufferedReader = null;

            LineGraphSeries<DataPoint> heartRateSeries = new LineGraphSeries<>();

            PointsGraphSeries<DataPoint> bradycardiaSeries = new PointsGraphSeries<>();
            bradycardiaSeries.setShape(PointsGraphSeries.Shape.TRIANGLE);
            bradycardiaSeries.setColor(Color.RED);

            String line = "";                  // Temp variable to store every row of CSV for processing
            String delimiter = ",";            // Delimiter "," since its a CSV file
            int line_count = 0;

            try
            {
                bufferedReader = new BufferedReader(new FileReader(filePath));
                while ((line = bufferedReader.readLine()) != null)
                {
                    line_count++;
                    String[] heartRateRow = line.split(delimiter);
                    Integer currentHeartRate = Integer.parseInt(heartRateRow[0]);
                    heartRateSeries.appendData(new DataPoint(line_count, currentHeartRate),
                            true, 10000);
                    heartRate.add(currentHeartRate);
                    if(currentHeartRate < threshold)
                    {
                        bradycardia_flag = true;
                        bradycardiaMinutes.add(true);
                        bradycardiaSeries.appendData(new DataPoint(line_count, currentHeartRate),
                                false, 500);

                    }
                    else
                    {
                        bradycardiaMinutes.add(false);
                    }
                }
            }
            catch (FileNotFoundException e)
            {
                e.printStackTrace();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
            finally
            {
                if (bufferedReader != null)
                {
                    try
                    {
                        bufferedReader.close();
                    }
                    catch (IOException e)
                    {
                        e.printStackTrace();
                    }
                }
            }
            endtime = System.nanoTime();
            heartRateGraph.addSeries(heartRateSeries);
            heartRateGraph.getViewport().setScrollable(true);
            heartRateGraph.addSeries(bradycardiaSeries);
            return null;
        }

        protected void onProgressUpdate(String... value)
        {
            super.onProgressUpdate(value);
        }

        protected void onPostExecute(Void aVoid)
        {
            if(!fileError)
            {
                super.onPostExecute(aVoid);
                heartRateGraph.setVisibility(View.VISIBLE);
                heartRateGraph.getViewport().setXAxisBoundsManual(true);
                heartRateGraph.getViewport().setMinX(0);
                heartRateGraph.getViewport().setMaxX(60);
                heartRateGraph.getViewport().setScrollable(true);
                if(bradycardia_flag)
                {
                    result.setText("The bradycardia detected for the given user in the following slots: ");
                    ArrayList<String> minutes = new ArrayList<String>();
                    for(int i=0; i<bradycardiaMinutes.size(); i++) {
                        if(bradycardiaMinutes.get(i) == true) {
                            minutes.add(Integer.toString(i+1));
                        }
                    }
                    String resultMinutesString = "Minutes [ ";
                    for(String minute : minutes) {
                        resultMinutesString += (minute + " , ");
                    }
                    resultMinutesString = resultMinutesString.substring(0,resultMinutesString.length()-2)+"]";
                    resultMinutes.setText(resultMinutesString);

                }
                else {
                    result.setText("The patient ecg data is normal and doesn't have bradycardia!");
                }
                long timediff = endtime - startTime;
                timediff /= 1000000;
                timeElapsed.setText("The execuetion time for bradycardia detection is " + timediff);
            }
            else
            {
                nameError.setText("Please enter a valid filename");
            }

        }
    }


    private class PredictTask extends AsyncTask<Void, String, Void>
    {
        LineGraphSeries<DataPoint> heartRateSeries = new LineGraphSeries<>();
        ArrayList<Interval> preBrad = new ArrayList<Interval>();
        ArrayList<Double> heartRateVariance = new ArrayList<Double>();
        PointsGraphSeries<DataPoint> bradycardiaSeries = new PointsGraphSeries<>();
        Boolean varianceFlag = false;
        @Override
        protected void onPreExecute()
        {
            super.onPreExecute();
            heartRateGraph.removeAllSeries();
            nameError.setText("");

            result.setVisibility(View.VISIBLE);
            resultMinutes.setVisibility(View.VISIBLE);
            timeElapsed.setVisibility(View.VISIBLE);

            result.setText("");
            resultMinutes.setText("");
            timeElapsed.setText("");
        }

        protected Void doInBackground(Void... params)
        {
            if(!isDetectPressed)
            {
                return null;
            }
            int minuteIndex = 0;
            int minutes = heartRate.size();
            //preBrad= new ArrayList<Integer>();

            bradycardiaSeries.setShape(PointsGraphSeries.Shape.TRIANGLE);
            bradycardiaSeries.setColor(Color.RED);

            int c = 0;
            while(minuteIndex < minutes)
            {
                c++;
                ArrayList<Double> interval = new ArrayList<>();
                for(int i = 0; minuteIndex < heartRate.size() && i < intervalLength; i++)
                {
                    interval.add(Double.parseDouble (heartRate.get(minuteIndex).toString()));
                    minuteIndex ++;
                }
                System.out.println(interval);
                Double variance = getVariance(interval);
                heartRateVariance.add(variance);
                if(variance >= 40)
                {
                    varianceFlag = true;
                    Interval a= new Interval((heartRateVariance.size()*5)-5, (heartRateVariance.size()*5) );
                    preBrad.add(a);
                    bradycardiaSeries.appendData(new DataPoint(c, variance), false, 500);

                }
            }
            //System.out.println(heartRateVariance);
            for(int i = 0; i < heartRateVariance.size(); i++)
            {
                heartRateSeries.appendData(new DataPoint(i+1, heartRateVariance.get(i)), false , 10000);
                //System.out.print(heartRateVariance.get(i) + ", ");
            }

            if(preBrad.size()>2)
            {
                preBrad = merge(preBrad);
            }
            return null;
        }

        protected void onProgressUpdate(String... value)
        {
            super.onProgressUpdate(value);
        }

        protected void onPostExecute(Void aVoid)
        {


            if(!isDetectPressed) {
                nameError.setText("Please perform detection!");
                return;
            }
            //isDetectPressed = false;
            heartRateGraph.setVisibility(View.VISIBLE);
            heartRateGraph.addSeries(heartRateSeries);
            heartRateGraph.addSeries(bradycardiaSeries);
            heartRateGraph.getViewport().setScrollable(true);
            heartRateGraph.getViewport().setMinX(0);
            heartRateGraph.getViewport().setMaxX(heartRate.size()/5);

            if(!varianceFlag)
            {
                result.setText("Patient ecg data is normal, hence no pre bradycardia events predicted");
                return;
            }


            result.setText("Pre Bradycardia Events occurring in the following intervals");

            String res = "";
            int i =1;
            for(Iterator<Interval> it = preBrad.iterator(); it.hasNext();)
            {
                Interval temp = it.next();
                res = res + "=>Interval " + i + " ["+ temp.start + " - " + temp.end + "]\n";
                res = res + "   Possible Bradycardia Interval ["+ temp.end + " - "+ (temp.end+5) +"]\n";
                i++;
            }

            resultMinutes.setText(res);
        }
    }


    public class Interval {
        int start;
        int end;
        Interval() { start = 0; end = 0; }
        Interval(int s, int e) { start = s; end = e; }
    }

    public ArrayList<Interval> merge(ArrayList<Interval> intervals)
    {
        if (intervals.size() <= 1)
            return intervals;

        // Sort by ascending starting point using an anonymous Comparator
        //intervals.sort((i1, i2) -> Integer.compare(i1.start, i2.start));

        ArrayList<Interval> result = new ArrayList<Interval>();
        int start = intervals.get(0).start;
        int end = intervals.get(0).end;

        for (Interval interval : intervals) {
            if (interval.start <= end) // Overlapping intervals, move the end if needed
                end = Math.max(end, interval.end);
            else {                     // Disjoint intervals, add the previous one and reset bounds
                result.add(new Interval(start, end));
                start = interval.start;
                end = interval.end;
            }
        }

        // Add the last interval
        result.add(new Interval(start, end));
        return result;
    }
}