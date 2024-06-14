# ezandroidutils
[![](https://jitpack.io/v/shiveshnavin/ezandroidutils.svg)](https://jitpack.io/#shiveshnavin/ezandroidutils)

## Usage
1. Place in root `build.gradle`
```gradle
allprojects {
		repositories {
			maven { url 'https://jitpack.io' }
		}
	}
```
For gradle kts, in `settings.gradle.kts`
```
pluginManagement {
    repositories {
	//...
        jcenter()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
    	//...
        maven(url = "https://jitpack.io")
        jcenter()
    }
}
```

2. Update gradle files

root `build.gradle`
```gradle
dependencies {
	classpath 'com.google.gms:google-services:4.3.13'
        classpath 'com.google.firebase:firebase-crashlytics-gradle:2.9.1'
	 ...
}
 ```
 
 
`app/build.gradle`
```gradle
dependencies {
	implementation 'com.github.shiveshnavin:ezandroidutils:1.2.0'
	...
}

// Place at the bottom of file
apply plugin: 'com.google.gms.google-services'
apply plugin: 'com.google.firebase.crashlytics'

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
```gradle
maven { url 'https://jitpack.io' }
```

Gradle KTS
```

    implementation(platform("com.google.firebase:firebase-bom:33.1.0"))
    implementation("com.github.shiveshnavin:ezandroidutils:1.2.0") {
        exclude(group = "com.google.firebase", module = "firebase-crashlytics")
    }
    implementation("com.google.firebase:firebase-crashlytics-ktx")
    implementation("com.google.firebase:firebase-analytics-ktx")
```
