# R8 rules for the release build (minify + shrinkResources).
#
# Libraries ship their own consumer rules, which R8 applies automatically, so none are copied here:
#   - Room (entities, DAOs), Play Billing
#   - Google Mobile Ads: play-services-ads / play-services-ads-api (AdMob)
#   - User Messaging Platform (UMP consent)
# They appear in app/build/outputs/mapping/release/configuration.txt after a release build.
# Add project-specific rules here only if something the app reflects on gets stripped.
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod
