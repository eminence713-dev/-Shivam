package com.example.data.model

data class DemoSeedEntry(
    val id: String,
    val product: Product,
    val sampleCostInr: Long
)

object DemoCatalog {
    fun buildSeedEntries(): List<DemoSeedEntry> {
        val baseTime = System.currentTimeMillis()
        return listOf(
            DemoSeedEntry(
                id = "demo_saree_banarasi",
                sampleCostInr = 780L,
                product = Product(
                    id = "demo_saree_banarasi",
                    nameEn = "Banarasi Art Silk Festive Saree",
                    nameHi = "बनारसी आर्ट सिल्क फेस्टिव साड़ी",
                    category = ProductCategory.SAREES.name,
                    descriptionEn = "Rich maroon woven border saree with blouse piece, suitable for weddings and festive occasions.",
                    descriptionHi = "ब्लाउज पीस के साथ आकर्षक मैरून ज़री बॉर्डर साड़ी — शादी और त्योहारों के लिए उत्तम।",
                    sellingPriceInr = 1150L,
                    originalPriceInr = 1499L,
                    stockCount = 8L,
                    lowStockThreshold = 2L,
                    sizes = listOf("Free Size (5.5m + Blouse)"),
                    colors = listOf("Maroon Gold", "Emerald Gold"),
                    sampleImageKey = "saree",
                    isPublished = true,
                    isFeatured = true,
                    isDemo = true,
                    createdAtMillis = baseTime - 1_000L,
                    updatedAtMillis = baseTime - 1_000L
                )
            ),
            DemoSeedEntry(
                id = "demo_kurti_cotton",
                sampleCostInr = 320L,
                product = Product(
                    id = "demo_kurti_cotton",
                    nameEn = "Embroidered Rayon Straight Kurti",
                    nameHi = "कढ़ाई वाली रेयॉन कुर्ती",
                    category = ProductCategory.KURTIS.name,
                    descriptionEn = "Comfortable breathable daily and festive wear kurti with thread embroidery on neckline.",
                    descriptionHi = "गले पर सुंदर कढ़ाई के साथ आरामदायक रेयॉन कुर्ती।",
                    sellingPriceInr = 499L,
                    originalPriceInr = 699L,
                    stockCount = 12L,
                    lowStockThreshold = 3L,
                    sizes = listOf("M", "L", "XL", "XXL"),
                    colors = listOf("Mustard", "Bottle Green", "Ivory"),
                    sampleImageKey = "kurti",
                    isPublished = true,
                    isFeatured = true,
                    isDemo = true,
                    createdAtMillis = baseTime - 2_000L,
                    updatedAtMillis = baseTime - 2_000L
                )
            ),
            DemoSeedEntry(
                id = "demo_frock_party",
                sampleCostInr = 460L,
                product = Product(
                    id = "demo_frock_party",
                    nameEn = "Festive Flared Ladies Frock Gown",
                    nameHi = "फेस्टिव फ्लेयर्ड लेडीज़ फ्रॉक",
                    category = ProductCategory.LADIES_FROCKS.name,
                    descriptionEn = "Full-length flared frock with soft lining and subtle gota trim.",
                    descriptionHi = "सॉफ्ट लाइनिंग और गोटा बॉर्डर के साथ लेडीज़ फ्लेयर्ड फ्रॉक।",
                    sellingPriceInr = 749L,
                    originalPriceInr = 950L,
                    stockCount = 6L,
                    lowStockThreshold = 2L,
                    sizes = listOf("S", "M", "L", "XL"),
                    colors = listOf("Wine Maroon", "Mustard Gold"),
                    sampleImageKey = "frock",
                    isPublished = true,
                    isFeatured = false,
                    isDemo = true,
                    createdAtMillis = baseTime - 3_000L,
                    updatedAtMillis = baseTime - 3_000L
                )
            ),
            DemoSeedEntry(
                id = "demo_shirt_cotton",
                sampleCostInr = 310L,
                product = Product(
                    id = "demo_shirt_cotton",
                    nameEn = "Men's Pure Cotton Check & Solid Shirt",
                    nameHi = "जेंट्स प्योर कॉटन शर्ट",
                    category = ProductCategory.SHIRTS.name,
                    descriptionEn = "Crisp regular-fit full sleeve cotton shirt for office and family gatherings.",
                    descriptionHi = "ऑफिस और पारिवारिक अवसरों के लिए फुल स्लीव कॉटन शर्ट।",
                    sellingPriceInr = 480L,
                    originalPriceInr = 650L,
                    stockCount = 14L,
                    lowStockThreshold = 3L,
                    sizes = listOf("38 (M)", "40 (L)", "42 (XL)"),
                    colors = listOf("Cream Check", "Navy Blue", "Olive"),
                    sampleImageKey = "shirt",
                    isPublished = true,
                    isFeatured = true,
                    isDemo = true,
                    createdAtMillis = baseTime - 4_000L,
                    updatedAtMillis = baseTime - 4_000L
                )
            ),
            DemoSeedEntry(
                id = "demo_tshirt_polo",
                sampleCostInr = 190L,
                product = Product(
                    id = "demo_tshirt_polo",
                    nameEn = "Soft Cotton Collar Polo T-Shirt",
                    nameHi = "कॉटन कॉलर पोलो टी-शर्ट",
                    category = ProductCategory.TSHIRTS.name,
                    descriptionEn = "Everyday breathable cotton pique collar t-shirt.",
                    descriptionHi = "रोज़मर्रा पहनने के लिए आरामदायक कॉटन टी-शर्ट।",
                    sellingPriceInr = 299L,
                    originalPriceInr = 399L,
                    stockCount = 15L,
                    lowStockThreshold = 4L,
                    sizes = listOf("M", "L", "XL"),
                    colors = listOf("Maroon", "Charcoal", "Sky Blue"),
                    sampleImageKey = "tshirt",
                    isPublished = true,
                    isFeatured = false,
                    isDemo = true,
                    createdAtMillis = baseTime - 5_000L,
                    updatedAtMillis = baseTime - 5_000L
                )
            ),
            DemoSeedEntry(
                id = "demo_jeans_denim",
                sampleCostInr = 490L,
                product = Product(
                    id = "demo_jeans_denim",
                    nameEn = "Classic Comfort Stretch Denim Jeans",
                    nameHi = "क्लासिक स्ट्रेच डेनिम जींस",
                    category = ProductCategory.JEANS.name,
                    descriptionEn = "Durable mid-rise comfort stretch jeans with reinforced stitching.",
                    descriptionHi = "मजबूत सिलाई वाली आरामदायक स्ट्रेच जींस।",
                    sellingPriceInr = 750L,
                    originalPriceInr = 999L,
                    stockCount = 10L,
                    lowStockThreshold = 3L,
                    sizes = listOf("30", "32", "34", "36"),
                    colors = listOf("Dark Indigo", "Classic Blue", "Jet Black"),
                    sampleImageKey = "jeans",
                    isPublished = true,
                    isFeatured = false,
                    isDemo = true,
                    createdAtMillis = baseTime - 6_000L,
                    updatedAtMillis = baseTime - 6_000L
                )
            ),
            DemoSeedEntry(
                id = "demo_trousers_formal",
                sampleCostInr = 390L,
                product = Product(
                    id = "demo_trousers_formal",
                    nameEn = "Smart Formal Cotton Blend Trousers",
                    nameHi = "फॉर्मल कॉटन ब्लेंड ट्राउज़र",
                    category = ProductCategory.TROUSERS.name,
                    descriptionEn = "Wrinkle-resistant straight fit formal trousers.",
                    descriptionHi = "स्ट्रेट फिट फॉर्मल पैंट / ट्राउज़र।",
                    sellingPriceInr = 599L,
                    originalPriceInr = 799L,
                    stockCount = 9L,
                    lowStockThreshold = 2L,
                    sizes = listOf("30", "32", "34", "36"),
                    colors = listOf("Beige Khaki", "Navy", "Charcoal Grey"),
                    sampleImageKey = "trousers",
                    isPublished = true,
                    isFeatured = false,
                    isDemo = true,
                    createdAtMillis = baseTime - 7_000L,
                    updatedAtMillis = baseTime - 7_000L
                )
            ),
            DemoSeedEntry(
                id = "demo_shoes_casual",
                sampleCostInr = 420L,
                product = Product(
                    id = "demo_shoes_casual",
                    nameEn = "Cushioned Walking & Casual Lace Shoes",
                    nameHi = "आरामदायक कैजुअल शूज़",
                    category = ProductCategory.SHOES.name,
                    descriptionEn = "Riya Fancy Shoes Mart collection — lightweight anti-skid sole shoes.",
                    descriptionHi = "रिया फैंसी शूज़ मार्ट कलेक्शन — हल्के और मजबूत सोल वाले जूते।",
                    sellingPriceInr = 650L,
                    originalPriceInr = 899L,
                    stockCount = 7L,
                    lowStockThreshold = 2L,
                    sizes = listOf("UK 6", "UK 7", "UK 8", "UK 9", "UK 10"),
                    colors = listOf("Tan Brown", "Navy", "Black"),
                    sampleImageKey = "shoes",
                    isPublished = true,
                    isFeatured = true,
                    isDemo = true,
                    createdAtMillis = baseTime - 8_000L,
                    updatedAtMillis = baseTime - 8_000L
                )
            ),
            DemoSeedEntry(
                id = "demo_sandals_fancy",
                sampleCostInr = 270L,
                product = Product(
                    id = "demo_sandals_fancy",
                    nameEn = "Riya Fancy Beaded Wedge Ladies Sandals",
                    nameHi = "रिया फैंसी लेडीज़ सैंडल",
                    category = ProductCategory.SANDALS.name,
                    descriptionEn = "Traditional golden beadwork comfort sandals for festive and daily wear.",
                    descriptionHi = "त्योहार और रोज़मर्रा के लिए सुंदर लेडीज़ सैंडल।",
                    sellingPriceInr = 425L,
                    originalPriceInr = 550L,
                    stockCount = 11L,
                    lowStockThreshold = 3L,
                    sizes = listOf("IND 5", "IND 6", "IND 7", "IND 8"),
                    colors = listOf("Antique Gold", "Maroon", "Rose Gold"),
                    sampleImageKey = "sandals",
                    isPublished = true,
                    isFeatured = true,
                    isDemo = true,
                    createdAtMillis = baseTime - 9_000L,
                    updatedAtMillis = baseTime - 9_000L
                )
            ),
            DemoSeedEntry(
                id = "demo_slippers_daily",
                sampleCostInr = 120L,
                product = Product(
                    id = "demo_slippers_daily",
                    nameEn = "Soft Ortho-Comfort Daily Family Slippers",
                    nameHi = "सॉफ्ट डेली वियर चप्पल / स्लिपर्स",
                    category = ProductCategory.SLIPPERS.name,
                    descriptionEn = "Durable washable soft-footbed slippers for home and market wear.",
                    descriptionHi = "घर और बाज़ार के लिए टिकाऊ और मुलायम चप्पल।",
                    sellingPriceInr = 199L,
                    originalPriceInr = 250L,
                    stockCount = 18L,
                    lowStockThreshold = 4L,
                    sizes = listOf("6", "7", "8", "9", "10"),
                    colors = listOf("Brown", "Grey", "Navy"),
                    sampleImageKey = "slippers",
                    isPublished = true,
                    isFeatured = false,
                    isDemo = true,
                    createdAtMillis = baseTime - 10_000L,
                    updatedAtMillis = baseTime - 10_000L
                )
            )
        )
    }
}
