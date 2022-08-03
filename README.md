# ezandroidutils
[![](https://jitpack.io/v/shiveshnavin/ezandroidutils.svg)](https://jitpack.io/#shiveshnavin/ezandroidutils)

## Usage
1. Place in root `build.gradle`
```
allprojects {
		repositories {
			maven { url 'https://jitpack.io' }
		}
	}
```
2. In app's `build.gradle`

```
implementation 'com.github.shiveshnavin:ezandroidutils:1.2.0'
```
3. Register on firebase and place `google-services.json` in app module
4. Extend `com.semibit.ezandroidutils.App` 
```java

public class App extends com.semibit.ezandroidutils.App {
	
	...

}
```
5. Add to manifest under application:name
```xml
<application
        android:name=".App"
        android:allowBackup="true">
	
	...
	
</application>
```

## Notes
If you get an error like 
```
Build was configured to prefer settings repositories over project repositories but repository 'maven' was added by build file 'build.gradle'
``` 

Undo Step 1 and Try addming the below config in `settings.gradle`
```
maven { url 'https://jitpack.io' }
```
