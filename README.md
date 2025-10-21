# Mumla

Fumla is a fork from Mumla, with the ambition to make the app more intuitive inspired 
by Zello and also add the ability to use programmable buttons for PTT during dark screen. 
The remaining part of this readme is copied from Mumla.
Mumla is again a fork and continuation of [Plumble](https://github.com/acomminos/Plumble),
a robust GPLv3 Mumble client for Android originally written by Andrew Comminos.
It uses the the [Humla](https://gitlab.com/quite/humla) protocol implementation
(forked from Comminos's [Jumble](https://github.com/acomminos/Jumble)).

## Repository submodules

Note that this Mumla git repository has submodule(s). You either need to clone
it using `git clone --recursive`, or you need to get the submodule(s) in place
after cloning:

    git submodule update --init --recursive

## Building on GNU/Linux

Building is verified to work using JDK 17. So you typically want to set and
export the JAVA_HOME environment variable like `export
JAVA_HOME=/usr/lib/jvm/java-17-openjdk`.

The Android SDK need to be specified as usual, for example by setting
`ANDROID_SDK_ROOT`, or writing it to local.properties as `echo
>local.properties sdk.dir=/home/user/Android/Sdk`

TODO: humla-spongycastle should be built as a sub-project of Humla's Gradle,
but currently isn't.

    git submodule update --init --recursive

    pushd libraries/humla/libs/humla-spongycastle
    ../../gradlew jar
    popd

    ./gradlew assembleDebug

If you get an error running out of Java heap space, try raising the -Xmx in
`./gradle.properties`.

### Notes on NDK

The NDK is the toolchain used for building the native code (C/C++) of Humla. We
specify the version needed using `ndkVersion` in
`libraries/humla/build.gradle`.

We currently use Android Gradle Plugin (AGP) version 8.x, which should come
bundled with NDK 25.1.8937393 that we currently use. It is typically installed
in a directory in `~/Android/Sdk/ndk/`. Using newer NDK might give build
errors. See also: https://developer.android.com/studio/projects/install-ndk

If Android Studio does not automatically install the mentioned version of the
NDK in the mentioned directory, then you may be able to get it installed by
using the SDK Manager:

- Click SDK Tools tab.
- Check "Show Package Details"
- In the list view, expand "NDK (Side by side)"
- Check 25.1.8937393
- Click OK

## License

Mumla's [LICENSE](LICENSE) is GNU GPL v3.
