package com.hypodiabetic.happ;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.hypodiabetic.happ.Objects.Integration;
import com.hypodiabetic.happ.Objects.Profile;
import com.hypodiabetic.happ.Objects.Treatments;
import com.hypodiabetic.happ.integration.IntegrationsManager;
import com.hypodiabetic.happ.integration.Objects.ObjectToSync;
import com.hypodiabetic.happ.integration.openaps.IOB;
import com.hypodiabetic.happ.services.FiveMinService;

import org.json.JSONObject;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;


public class EnterTreatment extends android.support.v4.app.FragmentActivity {

    //Enter treatment fragments
    eSectionsPagerAdapter eSectionsPagerAdapter;                                                    //will provide fragments for each of the sections
    static ViewPager eViewPager;                                                                    //The {@link ViewPager} that will host the section contents.

    //manual treatment
    Fragment manualEnterFragmentObject;
    private static Spinner spinner_treatment_type;
    private static Spinner spinner_notes;
    private static EditText editText_treatment_time;
    private static EditText editText_treatment_date;
    private static EditText editText_treatment_value;
    private static Treatments manualTreatment               = new Treatments();
    //wizard treatment
    Fragment bolusWizardFragmentObject;
    private static EditText wizardSuggestedBolus;
    private static EditText wizardSuggestedCorrection;
    private static EditText wizardCarbs;
    private static String bwpCalculations;
    private static Treatments wizzardBolusTreatment        = new Treatments();
    private static Treatments wizzardCarbTratment          = new Treatments();
    private static Treatments wizzardCorrectionTreatment   = new Treatments();

    //Treatment Lists
    SectionsPagerAdapter mSectionsPagerAdapter;                                                     //will provide fragments for each of the sections
    ViewPager mViewPager;                                                                           //The {@link ViewPager} that will host the section contents.
    Fragment todayTreatmentsFragmentObject;
    Fragment yestTreatmentsFragmentObject;
    Fragment activeTreatmentsFragmentObject;
    public static Long selectedListItemDB_ID;                                                       //Tracks the selected items Treatments DB ID
    public static HashMap selectedListItemID;                                                       //Tracks the selected items list ID
    public static Boolean listDirty=false;                                                          //Tracks if treatment lists are dirty and need to be reloaded


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_enter_treatment);

        //Treatment Lists
        // Create the adapter that will return a fragment .
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());
        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) this.findViewById(R.id.treatmentsPager);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        //Build Fragments
        todayTreatmentsFragmentObject   = new treatmentsListFragment();
        yestTreatmentsFragmentObject    = new treatmentsListFragment();
        activeTreatmentsFragmentObject  = new treatmentsListFragment();
        Bundle bundleToday = new Bundle();
        bundleToday.putString("LOAD", "TODAY");
        Bundle bundleYest = new Bundle();
        bundleYest.putString("LOAD", "YESTERDAY");
        Bundle bundleActive = new Bundle();
        bundleActive.putString("LOAD", "ACTIVE");
        todayTreatmentsFragmentObject.setArguments(bundleToday);
        yestTreatmentsFragmentObject.setArguments(bundleYest);
        activeTreatmentsFragmentObject.setArguments(bundleActive);

        //Enter treatment fragments
        eSectionsPagerAdapter = new eSectionsPagerAdapter(getSupportFragmentManager());
        // Set up the ViewPager with the sections adapter.
        eViewPager = (ViewPager) this.findViewById(R.id.enterTreatmentsPager);
        eViewPager.setAdapter(eSectionsPagerAdapter);
        //Build Fragments
        bolusWizardFragmentObject   = new boluesWizardFragment();
        manualEnterFragmentObject   = new manualTreatmentFragment();

    }

    public void refreshListFragments(){
        final FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        if (activeTreatmentsFragmentObject.isAdded()) {
            ft.detach(activeTreatmentsFragmentObject);
            ft.attach(activeTreatmentsFragmentObject);
        }
        if (todayTreatmentsFragmentObject.isAdded()) {
            ft.detach(todayTreatmentsFragmentObject);
            ft.attach(todayTreatmentsFragmentObject);
        }
        if (yestTreatmentsFragmentObject.isAdded()) {
            ft.detach(yestTreatmentsFragmentObject);
            ft.attach(yestTreatmentsFragmentObject);
        }
        ft.commit();
    }

    public void wizardShowCalc(View view){
        if (!bwpCalculations.equals("")) tools.showAlertText(bwpCalculations, this);
    }
    public void wizardAccept(View view){

        if (wizzardCarbTratment != null){
            wizzardCarbTratment.value                   = tools.stringToDouble(wizardCarbs.getText().toString());
            if (wizzardCarbTratment.value == 0)         wizzardCarbTratment = null;
        }
        if (wizzardBolusTreatment != null){
            wizzardBolusTreatment.value                 = tools.stringToDouble(wizardSuggestedBolus.getText().toString());
            if (wizzardBolusTreatment.value == 0)       wizzardBolusTreatment = null;
        }
        if (wizzardCorrectionTreatment != null){
            wizzardCorrectionTreatment.value            = tools.stringToDouble(wizardSuggestedCorrection.getText().toString());
            if (wizzardCorrectionTreatment.value == 0)  wizzardCorrectionTreatment = null;
        }

        saveTreatment(wizzardCarbTratment, wizzardBolusTreatment, wizzardCorrectionTreatment, view);
    }
    public void manualSave(View view){
        Date treatmentDateTime = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyyHH:mm", getResources().getConfiguration().locale);
        String treatmentDateTimeString;

        //gets the values the user has entered
        treatmentDateTimeString     = editText_treatment_date.getText().toString() + editText_treatment_time.getText().toString();

        try {
            treatmentDateTime = sdf.parse(treatmentDateTimeString);
        } catch (ParseException e) {
            Crashlytics.logException(e);
        }

        manualTreatment                   = new Treatments();
        manualTreatment.datetime          = treatmentDateTime.getTime();
        manualTreatment.datetime_display  = treatmentDateTime.toString();
        manualTreatment.note              = spinner_notes.getSelectedItem().toString();
        manualTreatment.type              = spinner_treatment_type.getSelectedItem().toString();
        manualTreatment.value             = tools.stringToDouble(editText_treatment_value.getText().toString());

        if (manualTreatment.value > 0){
            if (manualTreatment.type.equals("Carbs")){
                saveTreatment(manualTreatment,null,null,view);

            } else if (manualTreatment.type.equals("Insulin")){
                if (manualTreatment.note.equals("bolus")) {
                    saveTreatment(null,manualTreatment,null,view);

                } else if (manualTreatment.note.equals("correction")){
                    saveTreatment(null,null,manualTreatment,view);

                }
            }
        }
    }
    public void cancel(View view){
        Intent intentHome = new Intent(view.getContext(), MainActivity.class);
        intentHome.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        view.getContext().startActivity(intentHome);
    }

    //saves a new Treatment
    public void saveTreatment(final Treatments carbs,final Treatments bolus,final Treatments correction,final View v){

        if (bolus == null && correction == null && carbs != null){                                  //carbs to save only
            carbs.save();
            editText_treatment_value.setText("");
            wizardCarbs.setText("");
            IntegrationsManager.newCarbs(carbs);
            Toast.makeText(this, carbs.value + " " + carbs.type + " entered", Toast.LENGTH_SHORT).show();

            refreshListFragments();

            //update Stats
            startService(new Intent(this, FiveMinService.class));
        } else {                                                                                    //We have insulin to deliver

            pumpAction.setBolus(bolus, carbs, correction, v.getContext());
        }

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_enter_treatment, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class eSectionsPagerAdapter extends FragmentPagerAdapter {

        public eSectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            switch (position){
                case 0:
                    return bolusWizardFragmentObject;
                case 1:
                    return manualEnterFragmentObject;
                default:
                    return null;
            }
        }
        @Override
        public int getCount() {
            // Show 2 total pages.
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return "Bolus Wizard";
                case 1:
                    return "Manual Entry";
            }
            return null;
        }
    }
    public static class boluesWizardFragment extends Fragment {
        public boluesWizardFragment() {}

        private Button buttonAccept;
        private TextView bwDisplayIOBCorr;
        private TextView bwDisplayCarbCorr;
        private TextView bwDisplayBGCorr;


        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            final View rootView = inflater.inflate(R.layout.fragment_bolus_wizard, container, false);

            //Bolus wizard summaries
            bwDisplayIOBCorr    = (TextView) rootView.findViewById(R.id.bwDisplayIOBCorr);
            bwDisplayCarbCorr   = (TextView) rootView.findViewById(R.id.bwDisplayCarbCorr);
            bwDisplayBGCorr     = (TextView) rootView.findViewById(R.id.bwDisplayBGCorr);
            //Inputs
            wizardCarbs                 = (EditText) rootView.findViewById(R.id.wizardCarbValue);
            wizardSuggestedBolus        = (EditText) rootView.findViewById(R.id.wizardSuggestedBolus);
            wizardSuggestedCorrection   = (EditText) rootView.findViewById(R.id.wizardSuggestedCorrection);

            buttonAccept            = (Button) rootView.findViewById(R.id.wizardAccept);
            bwpCalculations = "";

            //Run Bolus Wizard on suggested carb amount change
            wizardCarbs.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                }

                @Override
                public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                    run_bw();
                }

                @Override
                public void afterTextChanged(Editable editable) {
                }
            });

            run_bw();
            return rootView;
        }
        public void run_bw(){
            Double carbValue = 0D;

            if (!wizardCarbs.getText().toString().equals("")){
                carbValue = tools.stringToDouble(wizardCarbs.getText().toString());
            }

            JSONObject bw = BolusWizard.bw(carbValue);

            //Bolus Wizard Display
            bwDisplayIOBCorr.setText(           bw.optString("net_biob", "error"));
            bwDisplayCarbCorr.setText(          bw.optString("insulin_correction_carbs", "error"));
            bwDisplayBGCorr.setText(            bw.optString("insulin_correction_bg", "error"));
            wizardSuggestedBolus.setText(       tools.round(bw.optDouble("suggested_bolus", 0), 1).toString());
            wizardSuggestedCorrection.setText(  tools.round(bw.optDouble("suggested_correction", 0), 1).toString());

            //Bolus Wizard Calculations
            bwpCalculations =   "carb correction \n" +
                                    bw.optString("insulin_correction_carbs_maths", "") + "\n\n" +
                                bw.optString("bgCorrection", "") + " bg correction" + "\n" +
                                    bw.optString("insulin_correction_bg_maths", "") + "\n\n" +
                                "net bolus iob \n" +
                                    bw.optString("net_biob_maths", "") + "\n\n" +
                                "suggested correction \n" +
                                    bw.optString("suggested_correction_maths", "") + "\n\n" +
                                "suggested bolus \n" +
                                    bw.optString("suggested_bolus_maths", "");


            Date dateNow = new Date();
            //if (bw.optDouble("suggested_bolus", 0D) > 0) {
                wizzardBolusTreatment                   = new Treatments();
                wizzardBolusTreatment.datetime          = dateNow.getTime();
                wizzardBolusTreatment.datetime_display  = dateNow.toString();
                wizzardBolusTreatment.note              = "bolus";
                wizzardBolusTreatment.type              = "Insulin";
                wizzardBolusTreatment.value             = bw.optDouble("suggested_bolus", 0D);
            //}
            //if (bw.optDouble("suggested_correction", 0D) > 0) {
                wizzardCorrectionTreatment                  = new Treatments();
                wizzardCorrectionTreatment.datetime         = dateNow.getTime();
                wizzardCorrectionTreatment.datetime_display = dateNow.toString();
                wizzardCorrectionTreatment.note             = "correction";
                wizzardCorrectionTreatment.type             = "Insulin";
                wizzardCorrectionTreatment.value            = bw.optDouble("suggested_correction", 0D);
            //}
            //if (carbValue > 0){
                wizzardCarbTratment                     = new Treatments();
                wizzardCarbTratment.datetime            = dateNow.getTime();
                wizzardCarbTratment.datetime_display    = dateNow.toString();
                wizzardCarbTratment.note                = "";
                wizzardCarbTratment.type                = "Carbs";
                wizzardCarbTratment.value               = carbValue;
            //}

            if (wizzardCarbTratment.value == 0 && wizzardBolusTreatment.value == 0 && wizzardCorrectionTreatment.value == 0){
                buttonAccept.setEnabled(false);
            } else {
                buttonAccept.setEnabled(true);
            }
        }

    }
    public static class manualTreatmentFragment extends Fragment {
        public manualTreatmentFragment() {}

        private DatePickerDialog treatmentDatePickerDialog;
        private TimePickerDialog treatmentTimePicker;
        private SimpleDateFormat dateFormatterDate;
        private SimpleDateFormat dateFormatterTime;

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            final View rootView = inflater.inflate(R.layout.fragment_manual_treatment, container, false);

            spinner_treatment_type      = (Spinner) rootView.findViewById(R.id.treatmentSpinner);
            spinner_notes               = (Spinner) rootView.findViewById(R.id.noteSpinner);
            editText_treatment_time     = (EditText) rootView.findViewById(R.id.treatmentTime);
            editText_treatment_date     = (EditText) rootView.findViewById(R.id.treatmentDate);
            editText_treatment_value    = (EditText) rootView.findViewById(R.id.treatmentValue);

            rootView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View view, boolean b) {
                    if(b) {
                        if (view == editText_treatment_date) {
                            treatmentDatePickerDialog.show();
                        } else if (view == editText_treatment_time) {
                            treatmentTimePicker.show();
                        }
                        //view.clearFocus();
                    }
                }
            });

            setupPickers(rootView);
            return rootView;
        }

        public void setupPickers(final View v){
            //setups the date, time, value and type picker

            Calendar newCalendar = Calendar.getInstance();

            //Type Spinner
            String[] treatmentTypes = {"Carbs", "Insulin"};
            ArrayAdapter<String> stringArrayAdapter= new ArrayAdapter<>(v.getContext(), android.R.layout.simple_spinner_dropdown_item, treatmentTypes);
            Spinner treatmentSpinner= (Spinner)v.findViewById(R.id.treatmentSpinner);
            treatmentSpinner.setAdapter(stringArrayAdapter);

            treatmentSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (parent.getSelectedItem().equals("Insulin")) {
                        String[] InsulinNotes = {"bolus", "correction"};
                        ArrayAdapter<String> stringArrayAdapter = new ArrayAdapter<>(v.getContext(), android.R.layout.simple_spinner_dropdown_item, InsulinNotes);
                        Spinner notesSpinner = (Spinner) v.findViewById(R.id.noteSpinner);
                        notesSpinner.setAdapter(stringArrayAdapter);
                    } else {
                        String[] EmptyNotes = {""};
                        ArrayAdapter<String> stringArrayAdapter = new ArrayAdapter<>(v.getContext(), android.R.layout.simple_spinner_dropdown_item, EmptyNotes);
                        Spinner notesSpinner = (Spinner) v.findViewById(R.id.noteSpinner);
                        notesSpinner.setAdapter(stringArrayAdapter);
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });

            //Value picker
            editText_treatment_value = (EditText) v.findViewById(R.id.treatmentValue);
            editText_treatment_value.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);

            //Date picker
            editText_treatment_date.setInputType(InputType.TYPE_NULL);
            editText_treatment_date.setOnFocusChangeListener(v.getOnFocusChangeListener());
            dateFormatterDate = new SimpleDateFormat("dd-MM-yyyy",  getResources().getConfiguration().locale);
            editText_treatment_date.setText(dateFormatterDate.format(newCalendar.getTime()));

            treatmentDatePickerDialog = new DatePickerDialog(v.getContext(), new DatePickerDialog.OnDateSetListener() {
                public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                    Calendar newDate = Calendar.getInstance();
                    newDate.set(year, monthOfYear, dayOfMonth);
                    editText_treatment_date.setText(dateFormatterDate.format(newDate.getTime()));
                }
            },newCalendar.get(Calendar.YEAR), newCalendar.get(Calendar.MONTH), newCalendar.get(Calendar.DAY_OF_MONTH));

            //Time Picker
            editText_treatment_time.setInputType(InputType.TYPE_NULL);
            dateFormatterTime = new SimpleDateFormat("HH:mm", getResources().getConfiguration().locale);
            editText_treatment_time.setText(dateFormatterTime.format(newCalendar.getTime()));
            editText_treatment_time.setOnFocusChangeListener(v.getOnFocusChangeListener());

            Calendar mcurrentTime = Calendar.getInstance();
            int hour = mcurrentTime.get(Calendar.HOUR_OF_DAY);
            int minute = mcurrentTime.get(Calendar.MINUTE);

            treatmentTimePicker = new TimePickerDialog(v.getContext(), new TimePickerDialog.OnTimeSetListener() {
                @Override
                public void onTimeSet(TimePicker timePicker, int selectedHour, int selectedMinute) {
                    editText_treatment_time.setText(selectedHour + ":" + selectedMinute);
                }
            }, hour, minute, true);//Yes 24 hour time
            treatmentTimePicker.setTitle("Select Time");
        }
    }


    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            switch (position){
                case 0:
                    return activeTreatmentsFragmentObject;
                case 1:
                    return todayTreatmentsFragmentObject;
                case 2:
                    return yestTreatmentsFragmentObject;
                default:
                    return null;
            }
        }

        @Override
        public int getCount() {
            // Show 3 total pages.
            return 3;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            if (listDirty) {
                refreshListFragments();
                listDirty = false;
            }
            switch (position) {
                case 0:
                    return "Active";
                case 1:
                    return "Today";
                case 2:
                    return "Yesterday";
            }
            return null;
        }
    }

    public static class treatmentsListFragment extends Fragment {
        public treatmentsListFragment(){}
        public ListView list;
        public mySimpleAdapter adapter;
        public View parentsView;
        public ArrayList<HashMap<String, String>> treatmentsList;

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            final View rootView = inflater.inflate(R.layout.fragment_treatments, container, false);

            loadTreatments(rootView);

            parentsView = rootView;
            return rootView;
        }

        public class mySimpleAdapter extends SimpleAdapter {

            public mySimpleAdapter(Context context, List<HashMap<String, String>> items, int resource, String[] from, int[] to) {
                super(context, items, resource, from, to);
            }
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);

                TextView treatmentListValue, treatmentListType;
                treatmentListType = (TextView) view.findViewById(R.id.treatmentTypeLayout);
                treatmentListValue = (TextView) view.findViewById(R.id.treatmentAmountLayout);
                if (treatmentListType.getText().equals("Carbs")){
                    treatmentListValue.setBackgroundResource(R.drawable.carb_treatment_round);
                    treatmentListValue.setText(tools.formatDisplayCarbs(Double.valueOf(treatmentListValue.getText().toString())));
                    treatmentListValue.setTextColor(getResources().getColor( R.color.primary_text));
                } else {
                    treatmentListValue.setBackgroundResource(R.drawable.insulin_treatment_round);
                    treatmentListValue.setText(tools.formatDisplayInsulin(Double.valueOf(treatmentListValue.getText().toString()), 1));
                    treatmentListValue.setTextColor(getResources().getColor(R.color.primary_light));
                }

                //Shows Integration details, if any
                ImageView treatmentInsulinIntegrationImage = (ImageView) view.findViewById(R.id.treatmentIntegrationIconLayout);
                TextView treatmentInsulinIntegrationText = (TextView) view.findViewById(R.id.treatmentIntegrationLayout);
                switch (treatmentInsulinIntegrationText.getText().toString()) {
                    case "sent":
                        treatmentInsulinIntegrationImage.setBackgroundResource(R.drawable.arrow_right_bold_circle);
                        break;
                    case "received":
                        treatmentInsulinIntegrationImage.setBackgroundResource(R.drawable.information);
                        break;
                    case "delayed":
                        treatmentInsulinIntegrationImage.setBackgroundResource(R.drawable.clock);
                        break;
                    case "delivered":
                        treatmentInsulinIntegrationImage.setBackgroundResource(R.drawable.checkbox_marked_circle);
                        break;
                    case "error":
                        treatmentInsulinIntegrationImage.setBackgroundResource(R.drawable.alert_circle);
                        break;
                    default:
                        treatmentInsulinIntegrationImage.setBackgroundResource(0);
                        break;
                }

                return view;
            }
        }

        public void loadTreatments(final View rootView){
            treatmentsList          = new ArrayList<>();
            List<Treatments> treatments;
            SimpleDateFormat sdfDate = new SimpleDateFormat("dd MMM", getResources().getConfiguration().locale);
            SimpleDateFormat sdfTime = new SimpleDateFormat("HH:mm", getResources().getConfiguration().locale);
            Calendar calDate        = Calendar.getInstance();
            Calendar treatmentDate  = Calendar.getInstance();
            Calendar calYesterday   = Calendar.getInstance();
            calYesterday.add(Calendar.DAY_OF_YEAR, -1); // yesterday
            Profile profile = new Profile(new Date());
            Boolean lastCarb=false,lastInsulin=false;

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainApp.instance());

            String toLoad="";
            Bundle bundle = this.getArguments();
            if (bundle != null) {
                toLoad = bundle.getString("LOAD", "TODAY");
            }

            switch (toLoad){
                case "TODAY":
                    treatments = Treatments.getTreatmentsDated(tools.getStartOfDayInMillis(calDate.getTime()),tools.getEndOfDayInMillis(calDate.getTime()), null);
                    break;
                case "YESTERDAY":
                    treatments = Treatments.getTreatmentsDated(tools.getStartOfDayInMillis(calYesterday.getTime()),tools.getEndOfDayInMillis(calYesterday.getTime()), null);
                    break;
                default: //all active
                    treatments = Treatments.getTreatmentsDated(calDate.getTimeInMillis() - (8 * 60 * 60000),calDate.getTimeInMillis(), null); //Grab all for the last 8 hours, we will look later if they are active
            }

            for (Treatments treatment : treatments){                                                    //Convert from a List<Object> Array to ArrayList
                HashMap<String, String> treatmentItem = new HashMap<>();

                if (treatment.datetime != null){
                    treatmentDate.setTime(new Date(treatment.datetime));
                } else {
                    treatmentDate.setTime(new Date(0));                                                 //Bad Treatment
                }
                treatmentItem.put("id", treatment.getId().toString());
                treatmentItem.put("time", sdfTime.format(treatmentDate.getTime()));
                treatmentItem.put("value", treatment.value.toString());
                treatmentItem.put("type", treatment.type);
                treatmentItem.put("note", treatment.note);
                treatmentItem.put("active", "");

                //Loads the remaining amount of activity for the treatment, if any
                if (treatment.type.equals("Insulin")){
                    if (!lastInsulin) {
                        String is_active = treatment.isActive(profile);

                        if (!is_active.equals("Not Active")) {                                      //Still active Insulin
                            treatmentItem.put("active", is_active);
                        } else {                                                                    //Not active
                            lastInsulin = true;
                        }
                    }

                    Integration integration = Integration.getIntegration("insulin_integration_app","bolus_delivery",treatment.getId());
                    treatmentItem.put("integration", integration.state);  //log STATUS of insulin_Integration_App

                } else {
                    if (!lastCarb) {
                        String is_active = treatment.isActive(profile);

                        if (!is_active.equals("Not Active")) {                                      //Still active carbs
                            treatmentItem.put("active", is_active);
                        } else {                                                                    //Not active
                            lastCarb = true;
                        }
                    }
                    treatmentItem.put("integration", "");
                }

                if (toLoad.equals("ACTIVE")){
                    if (treatment.type.equals("Insulin")){
                        if (!lastInsulin ) treatmentsList.add(treatmentItem);
                    } else {
                        if (!lastCarb) treatmentsList.add(treatmentItem);
                    }
                } else {
                    treatmentsList.add(treatmentItem);
                }

            }

            list = (ListView) rootView.findViewById(R.id.treatmentList);
            adapter = new mySimpleAdapter(rootView.getContext(), treatmentsList, R.layout.treatments_list_layout,
                    new String[]{"id", "time", "value", "type", "note", "active", "integration"},
                    new int[]{R.id.treatmentID, R.id.treatmentTimeLayout, R.id.treatmentAmountLayout, R.id.treatmentTypeLayout, R.id.treatmentNoteLayout, R.id.treatmentActiveLayout, R.id.treatmentIntegrationLayout});
            list.setAdapter(adapter);

            list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    //TextView textview = (TextView) view.findViewById(R.id.treatmentID);
                    //String info = textview.getText().toString();

                    Toast.makeText(rootView.getContext(), "Long press for options", Toast.LENGTH_LONG).show();
                }
            });
            registerForContextMenu(list);   //Register popup menu when clicking a ListView item

            Log.d("DEBUG", "loadTreatments: " + treatments.size());
        }

        @Override
        public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
            super.onCreateContextMenu(menu, v, menuInfo);
            AdapterView.AdapterContextMenuInfo aInfo = (AdapterView.AdapterContextMenuInfo) menuInfo;

            // We know that each row in the adapter is a Map
            HashMap map =  (HashMap) adapter.getItem(aInfo.position);

            selectedListItemDB_ID   = Long.parseLong(map.get("id").toString());
            selectedListItemID      = (HashMap) adapter.getItem(aInfo.position);

            menu.setHeaderTitle(map.get("value") + " " + map.get("type"));
            menu.add(1, 1, 1, "Edit");
            menu.add(1, 2, 2, "Delete");
            menu.add(1, 3, 3, "Integration Details");
        }
        // This method is called when user selects an Item in the Context menu
        @Override
        public boolean onContextItemSelected(MenuItem item) {
            if (getUserVisibleHint()) {                                                             //be sure we only action for the current Fragment http://stackoverflow.com/questions/5297842/how-to-handle-oncontextitemselected-in-a-multi-fragment-activity
                int itemId = item.getItemId();
                Treatments treatment = Treatments.getTreatmentByID(selectedListItemDB_ID);

                if (treatment != null) {

                    switch (itemId) {
                        case 1: //Edit - loads the treatment to be edited and delete the original
                            spinner_treatment_type.setSelection(getIndex(spinner_treatment_type, treatment.type));
                            spinner_notes.setSelection(getIndex(spinner_notes, treatment.note));
                            Date treatmentDate = new Date(treatment.datetime);
                            SimpleDateFormat sdfDate = new SimpleDateFormat("dd-MM-yyyy", getResources().getConfiguration().locale);
                            SimpleDateFormat sdfTime = new SimpleDateFormat("HH:mm", getResources().getConfiguration().locale);
                            editText_treatment_date.setText(sdfDate.format(treatmentDate));
                            editText_treatment_time.setText(sdfTime.format(treatmentDate));
                            editText_treatment_value.setText(treatment.value.toString());

                            treatment.delete();
                            treatmentsList.remove(selectedListItemID);
                            adapter.notifyDataSetChanged();
                            adapter.notifyDataSetInvalidated();
                            listDirty = true;
                            eViewPager.setCurrentItem(1); //load edit treatment fragment
                            //loadTreatments(parentsView);
                            Toast.makeText(parentsView.getContext(), "Original Treatment Deleted, re-save to add back", Toast.LENGTH_SHORT).show();
                            break;
                        case 2: //Delete
                            treatment.delete();
                            treatmentsList.remove(selectedListItemID);
                            adapter.notifyDataSetChanged();
                            adapter.notifyDataSetInvalidated();
                            listDirty = true;
                            //loadTreatments(parentsView);
                            Toast.makeText(parentsView.getContext(), "Treatment Deleted", Toast.LENGTH_SHORT).show();
                            break;
                        case 3: //Integration Details
                            String intergrationType;
                            if (treatment.type.equals("Insulin")){
                                intergrationType="bolus_delivery";
                            } else {
                                intergrationType="carbs";
                            }
                            List<Integration> integrations = Integration.getIntegrationsFor(intergrationType,treatment.getId());

                            final Dialog dialog = new Dialog(parentsView.getContext());
                            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                            dialog.setContentView(R.layout.integration_dialog);
                            dialog.setCancelable(true);
                            dialog.setCanceledOnTouchOutside(true);

                            ListView integrationListView        = (ListView) dialog.findViewById(R.id.integrationList);
                            ArrayList<HashMap<String, String>> integrationList = new ArrayList<>();
                            Calendar integrationDate  = Calendar.getInstance();
                            SimpleDateFormat sdfDateTime = new SimpleDateFormat("dd MMM HH:mm", getResources().getConfiguration().locale);

                            for (Integration integration : integrations){                                                    //Convert from a List<Object> Array to ArrayList
                                HashMap<String, String> integrationItem = new HashMap<>();

                                ObjectToSync objectSyncDetails = new ObjectToSync(integration);

                                if (objectSyncDetails.requested != null){
                                    integrationDate.setTime(objectSyncDetails.requested);
                                } else {
                                    integrationDate.setTime(new Date(0));                                                 //Bad integration
                                }
                                integrationItem.put("integrationType",      integration.type);
                                integrationItem.put("integrationWhat",      "Request sent: " + objectSyncDetails.getObjectSummary());
                                integrationItem.put("integrationDateTime",  sdfDateTime.format(integrationDate.getTime()));
                                integrationItem.put("integrationState",     "State: " + objectSyncDetails.state);
                                integrationItem.put("integrationAction",    "Action: " + objectSyncDetails.action);
                                integrationItem.put("integrationRemoteID",  "RemoteID: " + objectSyncDetails.remote_id.toString());
                                integrationItem.put("integrationDetails",   objectSyncDetails.details);

                                integrationList.add(integrationItem);
                            }

                            SimpleAdapter adapter = new SimpleAdapter(MainActivity.getInstace(), integrationList, R.layout.integration_list_layout,
                                    new String[]{"integrationType", "integrationWhat", "integrationDateTime", "integrationState", "integrationAction", "integrationRemoteID", "integrationDetails"},
                                    new int[]{R.id.integrationType, R.id.integrationWhat, R.id.integrationDateTime, R.id.integrationState, R.id.integrationAction, R.id.integrationRemoteID, R.id.integrationDetails});
                            integrationListView.setAdapter(adapter);

                            dialog.show();
                            break;
                    }
                }
                return true;
            }
            return false;
        }

        //returns the location of an item in a spinner
        private static int getIndex(Spinner spinner, String myString){
            int index = 0;
            for (int i=0;i<spinner.getCount();i++){
                if (spinner.getItemAtPosition(i).equals(myString)){
                    index = i;
                }
            }
            return index;
        }
    }

}
