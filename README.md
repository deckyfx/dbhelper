# dbhelper
[![](https://jitpack.io/v/deckyfx/dbhelper.svg)](https://jitpack.io/#deckyfx/dbhelper)

Adapterview made easy

Add it in your root build.gradle at the end of repositories:

```gradle
allprojects {
	repositories {
	...
		maven { url 'https://jitpack.io' }
	}
}
```
Add the dependency

```gradle
dependencies {
	compile 'org.greenrobot:greendao:3.2.0'
	compile 'com.github.deckyfx:dbhelper:-SNAPSHOT'
}
```

## Usage
In your Activity / Application
```java
...
DBHelper DB = new DBHelper(getApplicationContext(), DaoMaster.class, "app.db");
...

```

```java
...
DBHelper.EntityMapWrapper entity    = DB.getEntity(/* Entity Name */);
Property keyProperty                = entity.getProperty("key");
Object settings                     = entity.queryBuilder().where(keyProperty.eq(key)).limit(1).list();
/* Or */
ArrayList<Object> list              = entity.loadAll();
...

```

More sample is [here]

## Feature:

 *
