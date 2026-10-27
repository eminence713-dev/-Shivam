package com.example.util

import com.example.data.model.AppLanguage
import java.text.NumberFormat
import java.util.Locale

fun formatInr(amount: Long): String {
    val formatter = NumberFormat.getIntegerInstance(Locale.forLanguageTag("en-IN"))
    return "₹${formatter.format(amount)}"
}

class BiText(val en: String, val hi: String) {
    fun get(lang: AppLanguage): String = if (lang == AppLanguage.HINDI) hi else en
}

object AppStrings {
    val storeTitleTop = BiText(
        "श्री SHIVAM READYMADE & RIYA FANCY SHOES MART",
        "श्री शिवम रेडीमेड एंड रिया फैंसी शूज़ मार्ट"
    )
    val storeSubtitle = BiText(
        "Bada Bazar, Kalyanpur, Motihari, Bihar • Prop: Raju Kumar Gupta",
        "बड़ा बाज़ार, कल्याणपुर, मोतिहारी, बिहार • प्रो.: राजू कुमार गुप्ता"
    )
    val shopNow = BiText("Shop Now", "अभी खरीदें")
    val viewProducts = BiText("View Products", "सभी उत्पाद देखें")
    val addToCart = BiText("Add to Cart", "कार्ट में डालें")
    val buyNow = BiText("Buy Now", "तुरंत खरीदें")
    val placeOrder = BiText("Place Order", "ऑर्डर बुक करें")
    val orderOnWhatsApp = BiText("Order on WhatsApp", "व्हाट्सऐप पर ऑर्डर करें")
    val askOnWhatsApp = BiText("Chat on WhatsApp", "व्हाट्सऐप पर पूछें")
    val manageProducts = BiText("Manage Products", "उत्पाद प्रबंधन")
    val updateStock = BiText("Update Stock", "स्टॉक अपडेट करें")
    val viewOrders = BiText("View Orders", "ऑर्डर देखें")
    val printBill = BiText("Print Bill", "बिल प्रिंट करें")
    val dailySales = BiText("Daily Sales & Profit", "दैनिक बिक्री व मुनाफा")
    val expenses = BiText("Expenses", "दुकान खर्च")
    val storeSettings = BiText("Store Settings", "स्टोर सेटिंग्स")
    val offlineSale = BiText("Record Counter Sale", "दुकान बिक्री (बिल)")
    val setupGuide = BiText("Setup & Hosting Guide", "सेटअप व होस्टिंग गाइड")

    val navHome = BiText("Home", "होम")
    val navCategories = BiText("Catalogue", "कैटलॉग")
    val navCart = BiText("Cart", "कार्ट")
    val navOrders = BiText("My Orders", "मेरे ऑर्डर")
    val navAdmin = BiText("Owner", "मालिक पैनल")

    val searchHint = BiText(
        "Search sarees, kurtis, shirts, shoes, sandals...",
        "साड़ी, कुर्ती, शर्ट, जूते, सैंडल खोजें..."
    )
    val featuredHeading = BiText("Featured Collection", "विशेष संग्रह")
    val newArrivalsHeading = BiText("New Arrivals", "नए उत्पाद")
    val popularHeading = BiText("Popular in Store", "ग्राहकों की पसंद (वास्तविक बिक्री)")
    val allCategories = BiText("All Categories", "सभी श्रेणियाँ")

    val inStock = BiText("In Stock", "उपलब्ध है")
    val lowStock = BiText("Low Stock", "कम स्टॉक")
    val outOfStock = BiText("Out of Stock", "स्टॉक समाप्त")
    val demoBadge = BiText("DEMO SAMPLE", "डेमो सैंपल")

    val deliveryPolicyTitle = BiText("Delivery & Return Information", "डिलीवरी और वापसी जानकारी")
    val awaitingOwnerConfirmation = BiText(
        "Awaiting owner confirmation — Contact shop for exact charges",
        "मालिक द्वारा पुष्टि बाकी — शुल्क के लिए दुकान से संपर्क करें"
    )

    val whatsappOrderDisclaimer = BiText(
        "Note: Opening WhatsApp prepares a message for the shop. An online order is officially saved only when you tap 'Place Order' and see an SSR order reference.",
        "ध्यान दें: व्हाट्सऐप खोलने से केवल संदेश तैयार होता है। पक्का ऑनलाइन ऑर्डर तभी सेव होता है जब आप 'ऑर्डर बुक करें' दबाकर SSR रेफरेंस नंबर प्राप्त करते हैं।"
    )

    val errNameRequired = BiText(
        "Please enter customer full name (at least 2 characters).",
        "कृपया ग्राहक का पूरा नाम लिखें (कम से कम 2 अक्षर)।"
    )
    val errPhoneInvalid = BiText(
        "Please enter a valid 10-digit Indian mobile number starting with 6–9.",
        "कृपया 6–9 से शुरू होने वाला सही 10 अंकों का मोबाइल नंबर दर्ज करें।"
    )
    val errAddressRequired = BiText(
        "Please enter your complete village/ward delivery address (at least 5 characters).",
        "कृपया पूरा पता (वार्ड / गाँव / मोहल्ला) दर्ज करें।"
    )
}
