# kotlinx.serialization generates companion serializers that are only reached
# reflectively from the plugin-generated lookup, so R8 cannot see the link itself.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**

-keepclassmembers class com.glazkov.brakebedding.data.** {
    *** Companion;
}
-keepclasseswithmembers class com.glazkov.brakebedding.data.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# The sealed Stage hierarchy is resolved by its @SerialName discriminator at runtime.
-keep,includedescriptorclasses class com.glazkov.brakebedding.data.Stage
-keep,includedescriptorclasses class com.glazkov.brakebedding.data.**$$serializer { *; }

# Line numbers make Play Console crash reports readable while still obfuscating names.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
