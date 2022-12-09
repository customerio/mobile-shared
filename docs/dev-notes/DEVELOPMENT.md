# Development Getting Started

*Note: It's assumed that you have [Android Studio](https://developer.android.com/studio/) and [Kotlin Multiplatform Mobile plugin](https://plugins.jetbrains.com/plugin/14936-kotlin-multiplatform-mobile) installed on your machine before continuing. Read more about Kotlin Multiplatform Mobile instructions [here](https://kotlinlang.org/docs/multiplatform-mobile-getting-started.html)*

# Work on Remote Habits locally

We can test this repository with [Remote Habits](https://github.com/customerio/RemoteHabits-Android) just like we do for our [Android SDK](https://github.com/customerio/customerio-android/blob/develop/docs/dev-notes/DEVELOPMENT.md#work-on-remote-habits-locally).

If you are fimiliar with the setup already, follow these quick instructions to run KMM locally on Remote Habits:

- Checkout the desired branch in mobile-shared repository ([bq/queue](https://github.com/customerio/mobile-shared/tree/bq/queue) ideally)
- Run `install local` configuration in KMM project
- Checkout [`feat/kmm-bg-queue-v2`](https://github.com/customerio/customerio-android/tree/feat/kmm-bg-queue-v2) branch on Android
- Sync gradle files to make sure it is using the latest local release
- Run `install local` configuration in Android SDK
- Checkout the desired branch in RemoteHabits (You should be able to run on `main` too)
- Update `siteid` and `apikey` with devbox credentials
- Sync gradle files to make sure it is using the latest local releases
- Run the app
