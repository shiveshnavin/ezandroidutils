# ezandroidutils
[![](https://jitpack.io/v/shiveshnavin/ezandroidutils.svg)](https://jitpack.io/#shiveshnavin/ezandroidutils)

## Usage
Place in root `build.gradle`
```
allprojects {
		repositories {
			maven { url 'https://jitpack.io' }
		}
	}
```
And in apps `build.gradle`

```
implementation 'com.github.shiveshnavin:ezandroidutils:1.1.0b'
```

## Notes
If you get an error like `Build was configured to prefer settings repositories over project repositories but repository 'maven' was added by build file 'build.gradle'` .
Try addming `maven { url 'https://jitpack.io' }` in `settings.gradle`
