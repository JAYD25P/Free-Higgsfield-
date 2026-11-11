# OpenHiggsfield Android

An Android client shell for the OpenHiggsfield web studio. It opens the hosted studio by default and can be pointed at a self-hosted HTTPS instance.

## Features
- Native Android shell with a dark studio header.
- Loads the actual OpenHiggsfield web interface rather than simulating image/video generation.
- Configurable HTTPS studio URL for self-hosted deployments.
- WebView JavaScript and DOM storage support.
- File picker support for reference and media uploads.
- Download handling for generated media.
- Pull-to-refresh, progress indicator, navigation history and settings.
- API credentials are entered into the studio; this app does not embed provider secrets.

## Build APK
Every push to main and every manual workflow dispatch runs the Build OpenHiggsfield APK workflow.

1. Open the repository Actions tab.
2. Select the latest Build OpenHiggsfield APK run.
3. Download the OpenHiggsfield-debug-apk artifact.
4. Extract it and install app-debug.apk on Android.

The workflow uses JDK 17 and Gradle 8.9.

## Studio address
Default: https://openhiggsfield.ai

Use the top-right menu, then Studio settings, to switch to another HTTPS OpenHiggsfield deployment.

## Scope and licensing
This repository contains an independently written Android WebView client. It does not bundle or redistribute source code from wide-trace/open-higgsfield. The hosted studio and its generation providers remain subject to their own terms, licences, API requirements and usage charges. This Android wrapper does not make provider generations free or unlimited.
