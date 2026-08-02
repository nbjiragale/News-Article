# R8 rules for ArthaReader release builds.
#
# The app is a low-risk shrinking target: all JSON is parsed with explicit
# org.json optString/optJSONArray calls rather than reflective binding, so there
# is no model-class-name coupling to preserve. The rules below cover the two
# places where names DO survive into data or across process boundaries.

# Readable crash reports. Without SourceFile/LineNumberTable a release stack
# trace is just obfuscated frames, which makes the crash reporting planned in
# H7 close to useless. renamesourcefileattribute hides the original filename
# while keeping line numbers usable against a retained mapping.txt.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Enum constant names are persisted as TEXT in Room (saved_words.lookupMode and
# the cached_meanings cache key) and read back via valueOf(). Obfuscating them
# would make every previously saved word fail to deserialize after an update.
# SavedWordEntity.toSavedWord() swallows that failure with runCatching and
# defaults to Word, so the corruption would be silent -- every Sentence lookup
# quietly downgrading to Word.
-keepclassmembers enum com.niranjan.englisharticle.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Room entities are populated by generated code that references fields by name.
# Room ships consumer rules that already cover this; keeping the members
# explicitly is cheap insurance against a column/field mismatch that would only
# surface as a runtime SQLite error on a user's device.
-keepclassmembers class com.niranjan.englisharticle.data.local.*Entity {
    <fields>;
    <init>(...);
}
