# Room entities / backup models are accessed reflectively by Room & kotlinx.serialization.
-keep class ir.hesabdari.shop.data.** { *; }
-keepattributes *Annotation*, InnerClasses
-dontwarn org.slf4j.**
