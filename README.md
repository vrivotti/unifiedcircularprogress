# Unified Circular Progress  [ ![Download](https://api.bintray.com/packages/vrivotti/maven/unifiedcircularprogress/images/download.svg) ](https://bintray.com/vrivotti/maven/unifiedcircularprogress/_latestVersion) [![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

Circular progress bar with smooth transitions between determinate and indeterminate states. Looks like framework indeterminate circular `ProgressBar`, but with the ability to show determinate progress as well.

It works on Android 4.0+.

## Adding to project

```groovy
dependencies {
    compile 'io.github.vrivotti:unifiedcircularprogress:<latest-version>'
}
```

You may check the latest version [here](https://bintray.com/vrivotti/maven/unifiedcircularprogress/_latestVersion)

## How to use

Simply replace your `ProgressBar` with `UnifiedCircularProgressBar`.

This is not an extension of framework `ProgressBar`. You cannot change drawables and there is no secondary progress or background.

For example:

```xml
<io.github.vrivotti.unifiedcircularprogress.UnifiedCircularProgressBar
            android:id="@+id/progress"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_gravity="center"
            android:indeterminate="true"
            android:max="100"
            style="@style/Widget.AppCompat.ProgressBar"/>
```

