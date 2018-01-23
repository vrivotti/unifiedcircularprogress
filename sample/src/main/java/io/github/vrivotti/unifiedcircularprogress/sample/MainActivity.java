package io.github.vrivotti.unifiedcircularprogress.sample;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.AppCompatSeekBar;
import android.support.v7.widget.SwitchCompat;
import android.widget.CompoundButton;
import android.widget.SeekBar;

import io.github.vrivotti.unifiedcircularprogress.UnifiedCircularProgressBar;

public class MainActivity extends AppCompatActivity implements CompoundButton.OnCheckedChangeListener,
        AppCompatSeekBar.OnSeekBarChangeListener{

    private UnifiedCircularProgressBar progress;
    private SwitchCompat switch1;
    private AppCompatSeekBar seekBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        progress = findViewById(R.id.progress);
        switch1 = findViewById(R.id.switch1);
        seekBar = findViewById(R.id.seekBar);

        switch1.setOnCheckedChangeListener(this);
        switch1.setSwitchPadding(40);

        seekBar.setOnSeekBarChangeListener(this);
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        progress.setIndeterminate(switch1.isChecked());
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        progress.setProgress(seekBar.getProgress());
        switch1.setChecked(false);
    }
}
