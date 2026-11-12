# OLTNM - build, test and run shortcuts.
# Android Studio's SDK and bundled JDK are detected automatically.

ifeq ($(OS),Windows_NT)
GRADLEW := cmd /c gradlew.bat
EXE := .exe
STUDIO_JBR ?= C:/Program Files/Android/Android Studio/jbr
SDK_CANDIDATE := $(subst \,/,$(LOCALAPPDATA))/Android/Sdk
SDK_FALLBACK := $(subst \,/,$(USERPROFILE))/AppData/Local/Android/Sdk
SLEEP := cmd /c "timeout /t 5 /nobreak >NUL"
START_EMULATOR = cmd /c start "OLTNM emulator" "$(EMULATOR)" -avd $(AVD)
ANDROID_CLI = $(ANDROID_HOME)/cmdline-tools/latest/bin/android.exe
ANDROID_CLI_CMD = $(ANDROID_CLI)
SDKMANAGER_CMD = $(SDKMANAGER)
AVDMANAGER_CMD = $(AVDMANAGER)
LICENSES_CMD = $(SDKMANAGER) --sdk_root=$(ANDROID_HOME) --licenses
AVD_CREATE_CMD = $(AVDMANAGER) create avd -n "$(NEW_AVD)" -k "$(SYSTEM_IMAGE)" -d pixel_6 --force
else
GRADLEW := ./gradlew
EXE :=
STUDIO_JBR ?=
SDK_CANDIDATE := $(HOME)/Android/Sdk
SDK_FALLBACK :=
SLEEP := sleep 5
START_EMULATOR = $(EMULATOR) -avd $(AVD) &
ANDROID_CLI = $(ANDROID_HOME)/cmdline-tools/latest/bin/android
ANDROID_CLI_CMD = "$(ANDROID_CLI)"
SDKMANAGER_CMD = "$(SDKMANAGER)"
AVDMANAGER_CMD = "$(AVDMANAGER)"
LICENSES_CMD = "$(SDKMANAGER)" --sdk_root="$(ANDROID_HOME)" --licenses
AVD_CREATE_CMD = "$(AVDMANAGER)" create avd -n "$(NEW_AVD)" -k "$(SYSTEM_IMAGE)" -d pixel_6 --force
endif

# Prefer a caller-provided ANDROID_HOME, then Android Studio's default location.
ifndef ANDROID_HOME
ifneq ($(wildcard $(SDK_CANDIDATE)),)
ANDROID_HOME := $(SDK_CANDIDATE)
else ifneq ($(wildcard $(SDK_FALLBACK)),)
ANDROID_HOME := $(SDK_FALLBACK)
endif
endif

# Android Studio ships a JDK 21; use it when JAVA_HOME is not configured.
ifndef JAVA_HOME
ifneq ($(wildcard $(STUDIO_JBR)/bin/java$(EXE)),)
JAVA_HOME := $(STUDIO_JBR)
endif
endif

ifneq ($(ANDROID_HOME),)
export ANDROID_HOME
endif
ifneq ($(JAVA_HOME),)
export JAVA_HOME
endif

ADB ?= $(ANDROID_HOME)/platform-tools/adb$(EXE)
EMULATOR ?= $(ANDROID_HOME)/emulator/emulator$(EXE)
SDKMANAGER ?= $(ANDROID_HOME)/cmdline-tools/latest/bin/sdkmanager.bat
AVDMANAGER ?= $(ANDROID_HOME)/cmdline-tools/latest/bin/avdmanager.bat
JAVA_CMD ?= $(if $(JAVA_HOME),"$(JAVA_HOME)/bin/java$(EXE)",java)

# Modern cmdline-tools expose "android sdk install"; older ones use sdkmanager.
SDK_INSTALL = $(if $(wildcard $(ANDROID_CLI)),$(ANDROID_CLI_CMD) sdk install,$(SDKMANAGER_CMD) --sdk_root=$(ANDROID_HOME))

ifeq ($(OS),Windows_NT)
AVD_DIR := $(subst \,/,$(USERPROFILE))/.android/avd
else
AVD_DIR := $(HOME)/.android/avd
endif
FIRST_AVD := $(basename $(notdir $(firstword $(wildcard $(AVD_DIR)/*.ini))))

# Use the first AVD that already exists; "make avd" creates NEW_AVD instead.
AVD ?= $(if $(FIRST_AVD),$(FIRST_AVD),OLTNM)
NEW_AVD ?= OLTNM
SYSTEM_IMAGE ?= system-images;android-36;google_apis;x86_64
DEBUG_ACTIVITY := app.oltnm.debug/app.oltnm.MainActivity

.DEFAULT_GOAL := help
.PHONY: help sdk emulator-sdk licenses avd avds emulator wait-device build build-release install run run-emu devices test validate ci lint format format-check emojis clean doctor check-sdk

help:
	@echo Targets:
	@echo   make sdk            install the SDK 36 platform and build-tools
	@echo   make emulator-sdk   install the emulator and system image
	@echo   make avd            create the $(NEW_AVD) virtual device
	@echo   make emulator       start the $(AVD) emulator
	@echo   make run-emu        start emulator, install and launch the app
	@echo   make avds           list Android Virtual Devices
	@echo   make build          build the debug APK in app/build/outputs/apk/debug
	@echo   make build-release  build the minified release APK
	@echo   make install        install the debug APK on the connected device
	@echo   make run            install and launch on a connected device
	@echo   make devices        list connected Android devices
	@echo   make test           run the JVM unit tests
	@echo   make validate       build + unit tests + lint (no device needed)
	@echo   make ci             run the GitHub Actions validation locally
	@echo   make lint           run ktlint and Android lint
	@echo   make format         auto-format Kotlin sources with ktlint
	@echo   make format-check   fail if Kotlin sources are not formatted
	@echo   make emojis         regenerate Unicode emoji data from Emojibase
	@echo   make clean          delete Gradle build outputs
	@echo   make doctor         show SDK, Java, AVD and adb versions

check-sdk:
ifeq ($(ANDROID_HOME),)
	@echo ANDROID_HOME not found. Install Android Studio or set ANDROID_HOME.
	@exit 1
endif
ifeq ($(wildcard $(ANDROID_HOME)/platforms/android-36),)
	@echo Android SDK platform 36 not found in $(ANDROID_HOME). Run "make sdk".
	@exit 1
endif
ifeq ($(wildcard $(ANDROID_HOME)/build-tools/36.0.0),)
	@echo Android build-tools 36.0.0 not found in $(ANDROID_HOME). Run "make sdk".
	@exit 1
endif

licenses:
ifeq ($(ANDROID_HOME),)
	@echo ANDROID_HOME not found. Install Android Studio or set ANDROID_HOME.
	@exit 1
else
	$(LICENSES_CMD)
endif

sdk:
	$(SDK_INSTALL) "platforms;android-36"
	$(SDK_INSTALL) "build-tools;36.0.0"
	$(SDK_INSTALL) "platform-tools"

emulator-sdk:
	$(SDK_INSTALL) "emulator"
	$(SDK_INSTALL) "$(SYSTEM_IMAGE)"

avd: emulator-sdk
ifeq ($(ANDROID_HOME),)
	@echo ANDROID_HOME not found. Install Android Studio or set ANDROID_HOME.
	@exit 1
else
	$(AVD_CREATE_CMD)
endif

avds:
	$(AVDMANAGER_CMD) list avd

emulator:
ifeq ($(wildcard $(EMULATOR)),)
	@echo Emulator not found. Run "make sdk" first.
	@exit 1
else
	@echo Starting $(AVD)...
	$(START_EMULATOR)
endif

wait-device:
	"$(ADB)" wait-for-device
	$(SLEEP)

build: check-sdk
	$(GRADLEW) :app:assembleDebug

build-release: check-sdk
	$(GRADLEW) :app:assembleRelease

install: check-sdk
	$(GRADLEW) :app:installDebug

run: install
	"$(ADB)" shell am start -n $(DEBUG_ACTIVITY)

run-emu: emulator wait-device install
	"$(ADB)" shell am start -n $(DEBUG_ACTIVITY)

devices:
	"$(ADB)" devices

test: check-sdk
	$(GRADLEW) :app:testDebugUnitTest

validate: build test lint

ci: check-sdk
	$(GRADLEW) :app:testDebugUnitTest :app:ktlintCheck :app:lintDebug :app:assembleDebug

lint: check-sdk
	$(GRADLEW) :app:ktlintCheck :app:lintDebug

format:
	$(GRADLEW) :app:ktlintFormat

format-check:
	$(GRADLEW) :app:ktlintCheck

emojis:
	node scripts/generate-emojis.mjs

clean:
	$(GRADLEW) clean

doctor:
	@echo ANDROID_HOME=$(ANDROID_HOME)
	@echo JAVA_HOME=$(JAVA_HOME)
	@echo AVD=$(AVD)
	-$(JAVA_CMD) -version
	-$(GRADLEW) --version
	-"$(ADB)" version
	-$(AVDMANAGER_CMD) list avd
