package com.example.data.content

import com.example.data.model.ChutanoTechnique
import com.example.data.model.DefensePlanDay
import com.example.data.model.DefenseVideoItem
import com.example.data.model.EmergencyNumberItem
import com.example.data.model.GuideStepItem

object SelfDefenseCatalog {

    val emergencyNumbers: List<EmergencyNumberItem> = listOf(
        EmergencyNumberItem("জাতীয় জরুরি সেবা (পুলিশ / অ্যাম্বুলেন্স / ফায়ার)", "999", "২৪ ঘন্টা ফ্রি কল — যেকোনো বিপদে সাথে সাথে কল দিন"),
        EmergencyNumberItem("নারী ও শিশু নির্যাতন প্রতিরোধ হেল্পলাইন", "109", "২৪ ঘন্টা গোপন ও ফ্রি সহায়তা"),
        EmergencyNumberItem("হ্যালো পুলিশ / জরুরি সহায়তা ডেস্ক", "100", "তাৎক্ষণিক পুলিশ সহায়তা")
    )

    val badSituationGuide: List<GuideStepItem> = listOf(
        GuideStepItem(
            1,
            "১. মাথা ঠাণ্ডা রাখুন ও চারপাশ দেখুন",
            "আতঙ্কিত হয়ে থেমে যাবেন না। আশেপাশে মানুষ, দোকান, মসজিদ বা আলো কোথায় আছে ২ সেকেন্ডে দেখে নিন।"
        ),
        GuideStepItem(
            2,
            "২. দূরত্ব তৈরি ও জোরালো কণ্ঠ ('না' / চিৎকার)",
            "শক্ত গলায় বলুন 'দূরে সরো / বাঁচাও / আগুন!' — 'আগুন!' বললে আশেপাশের মানুষ দ্রুত বের হয়ে আসে।"
        ),
        GuideStepItem(
            3,
            "৩. দুর্বল জায়গায় আঘাত করে দৌড় দিন",
            "লড়াই নয়, পালানোই লক্ষ্য: আক্রমণকারীর চোখ, নাক, থুতনি বা হাঁটুতে হাতের তালু বা হাঁটু দিয়ে ধাক্কা দিয়েই দৌড় দিন।"
        ),
        GuideStepItem(
            4,
            "৪. নিরাপদ স্থানে পৌঁছে SOS ও কল",
            "ভীড় বা আলোর জায়গায় গিয়ে দ্রুত Noor এর Emergency SOS চাপুন এবং পরিবারের কাছে লোকেশন পাঠান।"
        )
    )

    val banglaVideos: List<DefenseVideoItem> = listOf(
        DefenseVideoItem(
            id = "vid_wrist",
            titleBn = "হাত ধরে ফেললে ২ সেকেন্ডে ছুটানোর কৌশল (Bangla)",
            durationText = "৩ মিনিট ভিডিও গাইড",
            summaryBn = "বৃদ্ধাঙ্গুলির দুর্বল দিক দিয়ে হাত ঘুরিয়ে টান দিয়ে ছুটানো ও দূরত্ব তৈরি।",
            searchQueryUrl = "https://www.youtube.com/results?search_query=self+defense+wrist+grab+escape+bangla"
        ),
        DefenseVideoItem(
            id = "vid_back",
            titleBn = "পিছন থেকে জাপটে ধরলে বাঁচার উপায় (Bangla)",
            durationText = "৪ মিনিট ভিডিও গাইড",
            summaryBn = "শরীর নিচু করে ভরকেন্দ্র নামানো, কনুই দিয়ে আঘাত ও পা ফাঁক করে ছুটানো।",
            searchQueryUrl = "https://www.youtube.com/results?search_query=self+defense+bear+hug+back+escape+bangla"
        ),
        DefenseVideoItem(
            id = "vid_orna",
            titleBn = "জামা বা ওড়না ধরে টানলে করণীয় (Bangla)",
            durationText = "৩ মিনিট ভিডিও গাইড",
            summaryBn = "টানের বিপরীতে না টেনে সামনের দিকে ঝুঁকে কলার/হাত ফ্রি করে ছিটকে সরে যাওয়া।",
            searchQueryUrl = "https://www.youtube.com/results?search_query=women+self+defense+basic+bangla+tutorial"
        )
    )

    val daily5MinDrills: List<String> = listOf(
        "১ মিনিট: হাত ছুটানোর রোটেশন প্র্যাকটিস (ডান ও বাম হাত ১০ বার)",
        "১ মিনিট: জাপটে ধরা থেকে নিচু হয়ে কনুইয়ের আঘাত (১০ বার)",
        "১ মিনিট: উচ্চস্বরে চিৎকার + স্ট্যান্স রেডি করা (শ্বাসের নিয়ন্ত্রণ)",
        "২ মিনিট: দ্রুত ১০ সেকেন্ড স্প্রিন্ট স্টার্ট + দিক পরিবর্তন ও রিফ্লেক্স"
    )

    val sevenDayPlan: List<DefensePlanDay> = listOf(
        DefensePlanDay(1, "দিন ১: স্ট্যান্স, সচেতনতা ও কণ্ঠ", "ভালো অবস্থান নেওয়া, হাত তুলে দূরত্ব রাখা ও জোর গলায় সতর্ক করা (৫ মিনিট)।"),
        DefensePlanDay(2, "দিন ২: এক হাত / দুই হাত ছুটানো", "আক্রমণকারীর বৃদ্ধাঙ্গুলির দিক বরাবর ঘুরিয়ে হাত ছুটানো ২০ বার অনুশীলন।"),
        DefensePlanDay(3, "দিন ৩: জামা / ওড়না ছুটানো কৌশল", "হালকা ঝুঁকে কাপড়ের টান আলগা করে হাত ছাড়িয়ে সরে যাওয়া।"),
        DefensePlanDay(4, "দিন ৪: পিছন থেকে ধরা থেকে মুক্তি", "ভরকেন্দ্র নিচু করা, পা দিয়ে টোকা ও কনুই দিয়ে ছুটানো।"),
        DefensePlanDay(5, "দিন ৫: চিৎকার + ১০ সেকেন্ড স্প্রিন্ট", "ছুটানোর পর পরই চিৎকার করে নিরাপদ জোন পর্যন্ত দ্রুত দৌড়।"),
        DefensePlanDay(6, "দিন ৬: কম্বিনেশন ড্রিল (হাত + চিৎকার + দৌড়)", "সব কৌশল একসাথে ৫ মিনিটে বিরতিহীনভাবে প্র্যাকটিস।"),
        DefensePlanDay(7, "দিন ৭: রিভিউ, SOS টেস্ট ও ফিটনেস", "পরিবারের সাথে Emergency SOS টেস্ট ও পুরো সপ্তাহের রিভিশন।")
    )

    val chutanoTechniques: List<ChutanoTechnique> = listOf(
        ChutanoTechnique(
            id = "hat_dhora",
            titleBn = "১. হাত ধরা থেকে ছুটানো (Wrist Grab Escape)",
            stepsBn = listOf(
                "কবজি টেনে পেছনে না নিয়ে আঙুলের ফাঁক (যেদিকে আক্রমণকারীর বুড়ো আঙুল থাকে) সেদিকে হাত ঘোরান।",
                "অন্য হাত দিয়ে নিজের মুঠো ধরে কনুই আপনার শরীরের দিকে দ্রুত ও জোরে ঝটকা টান দিন।",
                "হাত মুক্ত হওয়ামাত্র ১ পা পিছিয়ে দুহাত ওপরে তুলে গার্ড নিন এবং দৌড় দিন।"
            ),
            keyPointBn = "শক্তি নয়, বুড়ো আঙুলের দুর্বল দিক দিয়ে ঘোরালেই ২ সেকেন্ডে হাত ছুটে যাবে।"
        ),
        ChutanoTechnique(
            id = "jama_orna",
            titleBn = "২. জামা / ওড়না ধরা থেকে ছুটানো",
            stepsBn = listOf(
                "ওড়না বা কলার ধরে টানলে গলায় চাপ কমাতে প্রথমে আক্রমণকারীর দিকে সামান্য ঝুঁকে যান বা ওড়না আলগা করুন।",
                "এক হাতে তার কবজির ওপর শক্ত করে ধরে রাখুন এবং অন্য হাতের তালু দিয়ে তার কবজি/মুখের নিচে ধাক্কা দিন।",
                "প্রয়োজনে ওড়না ছেড়ে দিয়ে সরাসরি আলোর ও মানুষের দিকে দৌড় দিন — নিজের জীবন সবচেয়ে মূল্যবান।"
            ),
            keyPointBn = "কখনোই ধরে রাখা ওড়না পেছনে টানবেন না, সামান্য সামনে ঝুঁকলেই চাপ কমে যায়।"
        ),
        ChutanoTechnique(
            id = "pison_theke",
            titleBn = "৩. পিছন থেকে ধরা থেকে ছুটানো",
            stepsBn = listOf(
                "সাথে সাথে দুই হাঁটু ভাঁজ করে শরীর নিচু করুন ও পা ফাঁক করে ভরকেন্দ্র শক্ত করুন।",
                "শরীর নিচু করার মুহূর্তেই গোড়ালি দিয়ে তার পায়ের পাতায় চাপ দিন ও কনুই পেছনে ঝাপটা দিন।",
                "কোমরের বাঁধন আলগা হলেই পাশে ঘুরে ছিটকে বের হয়ে চিৎকারসহ দৌড় দিন।"
            ),
            keyPointBn = "শরীর নিচু করলে আক্রমণকারী আপনাকে শূন্যে তুলতে বা ধরে রাখতে পারবে না।"
        ),
        ChutanoTechnique(
            id = "sprint_10s",
            titleBn = "৪. ছুটাইয়া ১০ সেকেন্ড স্প্রিন্ট + চিৎকার দিয়া দৌড়",
            stepsBn = listOf(
                "ছুটানোর সাথে সাথে 'বাঁচাও! / আগুন!' বলে জোরে চিৎকার দিন যাতে আশেপাশের মানুষ তাকায়।",
                "পিছনে তাকিয়ে সময় নষ্ট না করে সোজা ১০ সেকেন্ড সর্বোচ্চ গতিতে নিরাপদ দোকান/জনবহুল দিকে দৌড়ান।",
                "নিচের '10 Sec Sprint Timer' চালু করে প্রতিদিন ২ বার এই ছুটানো + দৌড়ের অভ্যাস করুন।"
            ),
            keyPointBn = "সফল আত্মরক্ষার ৯০% হলো প্রথম ১০ সেকেন্ডে দ্রুত নিরাপদ দূরত্বে পৌঁছে যাওয়া!"
        )
    )
}
