package com.hashone.media.gallery

import com.hashone.commons.base.CommonApplication
import com.hashone.commons.languages.LanguageItem
import com.hashone.commons.languages.LocaleManager

class MyApplication: CommonApplication() {

    override fun onCreate() {
        super.onCreate()
        //TODO: App implementation
        setupAppLocale()
    }

    //TODO: App implementation
    private fun setupAppLocale() {
        LocaleManager.prepareLanguageList(
            arrayListOf(
                LanguageItem(languageOriginalName = "Arabic", languageCode = "ar", languageName = "عربي", isChecked = false),
                LanguageItem(languageOriginalName = "Bengali", languageCode = "bn", languageName = "বাংলা", isChecked = false),
                LanguageItem(languageOriginalName = "Bulgarian", languageCode = "bg", languageName = "български", isChecked = false),
                LanguageItem(languageOriginalName = "Croatian", languageCode = "hr", languageName = "Hrvatski", isChecked = false),
                LanguageItem(languageOriginalName = "Czech", languageCode = "cs", languageName = "čeština", isChecked = false),
                LanguageItem(languageOriginalName = "Danish", languageCode = "da", languageName = "dansk", isChecked = false),
                LanguageItem(languageOriginalName = "Dutch", languageCode = "nl", languageName = "Nederlands", isChecked = false),
                LanguageItem(languageOriginalName = "Filipino", languageCode = "fil", languageName = "Filipino", isChecked = false),
                LanguageItem(languageOriginalName = "Finnish", languageCode = "fi", languageName = "Suomalainen", isChecked = false),
                LanguageItem(languageOriginalName = "French", languageCode = "fr", languageName = "Français", isChecked = false),
                LanguageItem(languageOriginalName = "", languageCode = "en", languageName = "English", isChecked = false),
                LanguageItem(languageOriginalName = "German", languageCode = "de", languageName = "Deutsch", isChecked = false),
                LanguageItem(languageOriginalName = "Greek", languageCode = "el", languageName = "Ελληνικά", isChecked = false),
                LanguageItem(languageOriginalName = "Hindi", languageCode = "hi", languageName = "हिंदी", isChecked = false),
                LanguageItem(languageOriginalName = "Hungarian", languageCode = "hu", languageName = "Magyar", isChecked = false),
                LanguageItem(languageOriginalName = "Indonesian", languageCode = "id", languageName = "bahasa Indonesia", isChecked = false),
                LanguageItem(languageOriginalName = "Italian", languageCode = "it", languageName = "Italiano", isChecked = false),
                LanguageItem(languageOriginalName = "Japanese", languageCode = "ja", languageName = "日本語", isChecked = false),
                LanguageItem(languageOriginalName = "Khmer", languageCode = "km", languageName = "ខ្មែរ", isChecked = false),
                LanguageItem(languageOriginalName = "Korean", languageCode = "ko", languageName = "한국인", isChecked = false),
                LanguageItem(languageOriginalName = "Malay", languageCode = "ms", languageName = "Melayu", isChecked = false),
                LanguageItem(languageOriginalName = "Norwegian Bokmål", languageCode = "nb", languageName = "Norsk bokhvete", isChecked = false),
                LanguageItem(languageOriginalName = "Polish", languageCode = "pl", languageName = "Polski", isChecked = false),
                LanguageItem(languageOriginalName = "Portuguese", languageCode = "pt", languageName = "Português", isChecked = false),
                LanguageItem(languageOriginalName = "Romanian", languageCode = "ro", languageName = "Română", isChecked = false),
                LanguageItem(languageOriginalName = "Russian", languageCode = "ru", languageName = "Русский", isChecked = false),
                LanguageItem(languageOriginalName = "Simplified Chinese", languageCode = "zh-Hans", languageName = "简体中文", isChecked = false),
                LanguageItem(languageOriginalName = "Slovak", languageCode = "sk", languageName = "slovenský", isChecked = false),
                LanguageItem(languageOriginalName = "Spanish", languageCode = "es", languageName = "Española", isChecked = false),
                LanguageItem(languageOriginalName = "Swedish", languageCode = "sv", languageName = "svenska", isChecked = false),
                LanguageItem(languageOriginalName = "Thai", languageCode = "th", languageName = "แบบไทย", isChecked = false),
                LanguageItem(languageOriginalName = "Traditional Chinese", languageCode = "zh-Hant", languageName = "繁體中文", isChecked = false),
                LanguageItem(languageOriginalName = "Turkish", languageCode = "tr", languageName = "Türkçe", isChecked = false),
                LanguageItem(languageOriginalName = "Ukrainian", languageCode = "uk", languageName = "українська", isChecked = false),
                LanguageItem(languageOriginalName = "Vietnamese", languageCode = "vi", languageName = "Tiếng Việt", isChecked = false),
            )
        )
    }

}