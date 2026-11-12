# Aviator AI Assistant — Android starter prototype

यह Kotlin starter Android MediaProjection की स्पष्ट अनुमति लेकर स्क्रीन कैप्चर करता है और Google ML Kit OCR से स्क्रीन पर दिख रहे टेक्स्ट/multiplier-जैसे मान पहचानने की कोशिश करता है।

## चलाने का तरीका
1. ZIP extract करें।
2. Android Studio में `AviatorAIAndroid` फ़ोल्डर खोलें।
3. Gradle sync पूरा होने दें (इंटरनेट और Android SDK आवश्यक हैं)।
4. Android फोन पर ऐप रन करें।
5. `Start Live Screen Reading` दबाएँ और Android के screen-sharing prompt को स्वीकार करें।
6. फिर गेम पर जाएँ। Notification बताएगी कि screen reading चालू है।
7. बंद करने के लिए ऐप में वापस आकर `Stop` दबाएँ।

## सीमाएँ
- यह OCR demo है; अभी गेम-विशेष UI detection, overlay window, history database या cloud AI नहीं है।
- OCR हर गेम/स्क्रीन लेआउट पर सही काम नहीं करेगा।
- स्क्रीन कैप्चर केवल उपयोगकर्ता की स्पष्ट अनुमति से होता है।
- यह ऐप कोई bet नहीं लगाता, गेम को नियंत्रित नहीं करता, और अगले round का multiplier/crash time नहीं बताता।
- Android संस्करण/डिवाइस के अनुसार notification/foreground-service अनुमति की जरूरत पड़ सकती है।
- यह प्रोजेक्ट इस वातावरण में compile/test नहीं किया गया है; Android Studio में build करके डिवाइस पर जाँचना जरूरी है।

केवल उन गेम/साइटों पर उपयोग करें जिनकी शर्तें screen capture/analysis की अनुमति देती हैं। पासवर्ड, OTP या भुगतान जानकारी स्क्रीन पर हो तो capture रोक दें।
