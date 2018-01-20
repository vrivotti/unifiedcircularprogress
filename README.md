# unified-circular-progress

Circular progress bar with smooth transitions between determinate and indeterminate states. Looks like framework indeterminate circular `ProgressBar`, but with the ability to show determinate progress as well.

It works on Android 4.0+.

## How to use

Simply replace your `ProgressBar` with `UnifiedCircularProgressBar`.

This is not an extension of framework `ProgressBar`. You cannot change drawables and there is no secondary progress or background.

For example:

```xml
<io.github.verarivotti.unifiedcircularprogress.UnifiedCircularProgressBar
            android:id="@+id/progress"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_gravity="center"
            android:indeterminate="true"
            android:max="100"
            style="@style/Widget.AppCompat.ProgressBar"/>
```

