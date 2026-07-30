# kotlinx.serialization makes companion serializers. Only a generated lookup uses
# them, with reflection. R8 cannot see that connection.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**

-keepclassmembers class com.glazkov.brakebedding.data.** {
    *** Companion;
}
-keepclasseswithmembers class com.glazkov.brakebedding.data.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# The app finds the correct Stage type from its @SerialName value at run time.
-keep,includedescriptorclasses class com.glazkov.brakebedding.data.Stage
-keep,includedescriptorclasses class com.glazkov.brakebedding.data.**$$serializer { *; }

# The line numbers make Play Console crash reports readable. The names stay hidden.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
