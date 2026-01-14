package com.example.policemobiledirectory.utils



object Constants {



    const val LOCAL_CONSTANTS_VERSION = 3

    // ===============================

// RANKS

// ===============================

    val allRanksList = listOf(

        "APC", "CPC", "WPC", "PCW", "PC",

        "AHC", "CHC", "WHC", "HCW", "HC",

        "ASI", "ARSI", "WASI", "ASIW", "RSI",

        "PSI", "WPSI", "PSIW",

        "RPI", "CPI", "PI", "PIW", "WPI",

        "DYSP",

        "SDA", "FDA", "SS",

        "GHA", "AO", "Typist", "Steno", "PA"

    ).sorted()

    val ranksRequiringMetalNumber = setOf(

        "APC", "CPC", "WPC", "PC",

        "AHC", "CHC", "WHC", "HC"

    )
// ===============================

// BLOOD GROUPS

// ===============================
    val bloodGroupsList = listOf(

        "A+", "A-", "B+", "B-",

        "AB+", "AB-",

        "O+", "O-",

        "??"

    ).sorted()

    val districtsList = listOf(

        "Bagalkot -NR", "Ballari -BR", "Belagavi City -COP", "Belagavi Dist -NR",

        "Bengaluru City -COP", "Bengaluru Dist -CR", "Bidar -NR", "Chamarajanagar -SR",

        "Chikkaballapura -CR", "Chikkamagaluru -WR", "Chitradurga -ER", "Dakshina Kannada -WR",

        "Davanagere -ER", "Dharwad -NR", "Gadag -NR", "Hassan -SR", "Haveri -ER",

        "Hubballi Dharwad City -COP", "Kalaburagi -NER", "Kalaburagi City -COP", "KGF -CR",

        "Kodagu -SR", "Kolar -CR", "Koppal -BR", "Mandya -SR", "Mangaluru City -COP",

        "Mysuru City -COP", "Mysuru Dist -SR", "Raichur -BR", "Ramanagara -CR",

        "Shivamogga -ER", "Tumakuru -CR", "Udupi -WR", "Uttara Kannada -WR",

        "Vijayanagara -BR", "Vijayapura -NR", "Yadgir -NER"

    ).sorted()

    // ===============================
    // FUNCTIONAL / UNIT LIST
    // ===============================
    val defaultUnitsList = listOf(
        "Admin", "ASC Team", "BDDS", "C Room", "CAR",
        "CCB", "CCRB", "CDR", "CEN", "CID",
        "Coast Guard", "Computer", "Court", "CSB", "CSP",
        "DAR", "DCIB", "DCRB", "DCRE",
        "Dog Squad", "DSB", "ERSS", "ESCOM",
        "Excise", "Fire", "Forest", "FPB", "FRRO",
        "FSL", "Guest House", "Health", "Home Guard",
        "INT", "ISD", "KLA", "L&O",
        "Ministrial", "Others",
        "Prison", "PTS", "Railway", "RTO",
        "S INT", "Social Media", "Toll",
        "Traffic", "VVIP", "Wireless"
    ).sorted()

    val stationsByDistrictMap: Map<String, List<String>> = districtsList.associateWith { districtFullName ->



        val stations = when (districtFullName) {

            "Bagalkot -NR" -> listOf(

                "Amengad PS", "Badami PS", "Bagalkot CEN Crime PS", "Bagalkot Rural PS", "Bagalkot Town PS",

                "Bagalkot Traffic PS", "Bagalkot Women PS", "Banahatti PS", "Bilagi PS", "Guledagudda PS",

                "Hungunda PS", "Ilakal PS", "Ilakal Rural PS", "Jamakhandi Rural PS", "Jamakhandi Town PS",

                "Kaladagi PS", "Kerur PS", "Lokapur PS", "Mahalingapur PS", "Mudhol PS", "Navanagara PS",

                "Savalgi PS", "Teradal PS", "DPO", "Computer Sec", "DAR", "FPB", "MCU", "DCRB", "DSB",

                "Social Media", "State INT", "DCRE", "Lokayukta", "ESCOM", "C/Room"

            )

            "Ballari -BR" -> listOf(

                "APMC Yard PS", "Ballari CEN Crime PS", "Ballari Women PS", "Bellary Rural PS",

                "Bellary Traffic PS", "Brucepet PS", "Choranuru PS", "Cowlbazar PS", "Gandhinagar PS",

                "Hatcholli PS", "Kampli PS", "Kuduthini PS", "Kurugod PS", "Moka PS", "P.D. Halli PS",

                "Sandur PS", "Sirigeri PS", "Siruguppa PS", "Tekkalkota PS", "Thoranagal PS",

                "DPO", "Computer Sec", "DAR", "FPB", "MCU", "DCRB", "DSB", "Social Media",

                "State INT", "DCRE", "Lokayukta", "ESCOM", "C/Room"

            )

            "Belagavi City -COP" -> listOf(

                "APMC Yard PS", "Bagewadi PS", "Belagavi City CEN Crime PS", "Belagavi City Women PS",

                "Belagavi Rural PS", "Belgaum North Traffic PS", "Belgaum South Traffic PS", "Camp PS",

                "Kakati PS", "Khadebazar PS", "Malamaruthi PS", "Marihal PS", "Market PS", "Shahapura PS",

                "Tilakwadi PS", "Udyambag PS", "DPO", "Computer Sec", "CAR", "FPB", "MCU", "CCRB",

                "CSB", "Social Media", "State INT", "DCRE", "Lokayukta", "ESCOM", "C/Room"

            )

            "Belagavi Dist -NR" -> listOf(

                "Ankalgi PS", "Ankali PS", "Athani PS", "Bailahongal PS", "Basaweshwar Chowk PS",

                "Belagavi CEN Crime PS", "Belagavi Women PS", "Chikkodi PS", "Chikkodi Town Traffic PS",

                "Doddawad PS", "Ghataprabha PS", "Gokak Rural PS", "Gokak Town PS", "Harugeri PS",

                "Hukkeri PS", "Kagwad PS", "Katkol PS", "Khadakalat PS", "Khanapur PS", "Kittur PS",

                "Kudachi PS", "Kulgod PS", "Mudalgi PS", "Murgod PS", "Nandagad PS", "Nesargi PS",

                "Nippani Rural PS", "Nippani Town PS", "Raibag PS", "Ramadurga PS", "Sadalaga PS",

                "Sankeshwar PS", "Soundatti PS", "Yamakanmardi PS", "DPO", "Computer Sec", "DAR",

                "FPB", "MCU", "DCRB", "DSB", "Social Media", "State INT", "DCRE", "Lokayukta", "ESCOM", "C/Room"

            )

            "Bengaluru City -COP" -> listOf(

                "Adugodi PS", "Adugodi Traffic PS", "Airport Traffic PS", "Amruthahally PS", "Annapoorneshwari Nagar PS",

                "Ashoknagar PS", "Bagalagunte PS", "Bagalur PS", "Banasawadi Traffic PS", "Banashankari PS",

                "Banashankari Traffic PS", "Banaswadi PS", "Bandepalya PS", "Basavanagudi PS", "Basavanagudi Traffic PS",

                "Basavanagudi Women PS", "Basaveshwara Nagar PS", "Beguru PS", "Bellanduru PS", "Bellanduru Traffic PS",

                "Bharathi Nagar PS", "BIAL PS", "Bommanahalli PS", "Byadarahalli PS", "Byappanahalli PS",

                "Byatarayanapura PS", "Byatrarayanapura Traffic PS", "Central CEN Crime PS", "Chamarajpet PS",

                "Chandra Layout PS", "Channammanakere Achu Kattu PS", "Chickpet Traffic PS", "Chikkajala PS",

                "Chikkajala Traffic PS", "City Market PS", "City Market Traffic PS", "Commercial Street PS",

                "Cottonpet PS", "Cubbon Park PS", "Cubbonpark Traffic PS", "Cyber Crime Police Station",

                "Devanahalli PS", "Devanahalli Traffic PS", "Devarajeevanahalli PS", "East CEN Crime PS",

                "East Zone Women PS", "Electronic City PS", "Electronic City Traffic PS", "Gangammana Gudi PS",

                "Girinagar PS", "Govindapura PS", "Govindaraja Nagar PS", "H.A.L. PS", "Halasur Gate Traffic PS",

                "Halasur PS", "Halasur Traffic PS", "Halasurgate PS", "Hanumanthanagar PS", "Hebbal PS",

                "Hebbal Traffic PS", "Hennur PS", "Hennur Traffic PS", "High Grounds PS", "High Grounds Traffic PS",

                "HSR Lay Out Traffic PS", "HSR Layout PS", "Hulimavu PS", "Hulimavu Traffic PS", "Indiranagar PS",

                "J.C. Nagar PS", "Jagajeevanram Nagar PS", "Jalahalli PS", "Jalahalli Traffic PS", "Jayanagar PS",

                "Jayanagar Traffic PS", "Jayaprakash Nagar PS", "Jeevan Bheemanagar PS", "Jeevan Bhima Nagar Traffic PS",

                "Jnanabharathi PS", "K G Halli Traffic PS", "K.R. Puram PS", "K.R.Puram Traffic PS", "Kadugodi PS",

                "Kadugondana Halli PS", "Kalasipalya PS", "Kamakshipalya PS", "Kamakshipalya Traffic PS",

                "Kempapura Agrahara PS", "Kempegowda Nagar PS", "Kengeri PS", "Kengeri Traffic PS", "Kodigehalli PS",

                "Konanakunte PS", "Koramangala PS", "Kothanur PS", "Kumaraswamy Layout PS", "Kumaraswamy Layout Traffic PS",

                "Madivala PS", "Madivala Traffic PS", "Magadi Road PS", "Magadi Road Traffic PS", "Mahadevapura PS",

                "Mahadevapura Traffic PS", "Mahalakshmipuram PS", "Malleshwaram PS", "Malleshwaram Traffic PS",

                "Marathahalli PS", "Mico Layout PS", "Mico Layout Traffic PS", "Nandini Layout PS", "North CEN Crime PS",

                "NorthEast CEN Crime PS", "Parappana Agrahara", "Peenya PS", "Peenya Traffic PS", "Pulakeshinagar PS",

                "Pulakeshinagar Traffic PS", "Puttenahalli PS", "R.M.C. Yard PS", "R.T. Nagar PS", "R.T.Nagar Traffic PS",

                "Rajagopal Nagar PS", "Rajajinagar PS", "Rajajinagar Traffic PS", "Rajarajeshwari Nagar PS",

                "Ramamurthy Nagar PS", "S.J. Park PS", "Sadashivanagar PS", "Sadashivanagar Traffic PS",

                "Sampangiramanagar PS", "Sampigehalli PS", "Sanjay Nagar PS", "Seshadripuram PS", "Shankarpura PS",

                "Shivajinagar PS", "Shivajinagar Traffic PS", "Siddapura PS", "Soladevanahalli PS", "South CEN Crime PS",

                "SouthEast CEN Crime PS", "Srirampura PS", "Subramanyanagar PS", "Subramanyapura PS", "Suddaguntepalya PS",

                "Thalaghattapura PS", "Thalaghattapura Traffic PS", "Thilaknagar PS", "Upparpet PS", "Upparpet Traffic PS",

                "V V Puram Traffic PS", "Varthur PS", "Vidhana Soudha PS", "Vidyaranyapura PS", "Vijayanagar PS",

                "Vijayanagar Traffic PS", "Vishveshwarapuram PS", "Viveknagar PS", "Vyalikaval PS", "West CEN Crime PS",

                "Whitefield CEN Crime PS", "Whitefield PS", "Whitefield Traffic PS", "Wilsongarden PS",

                "Wilsongarden Traffic PS", "Yelahanka New Town PS", "Yelahanka PS", "Yelahanka Traffic PS",

                "Yeshwanthapura PS", "Yeshwanthapura Traffic PS", "DPO", "Computer Sec", "CAR", "FPB", "MCU",

                "CCRB", "CSB", "Social Media", "State INT", "DCRE", "Lokayukta", "ESCOM", "C/Room"

            )

            "Bengaluru Dist -CR" -> listOf(

                "Anekal PS", "Anugondanahalli PS", "Attibele PS", "Avalahally PS", "Bannerghatta PS",

                "Bengaluru CEN Crime PS", "Bengaluru Dist Women PS", "Chennarayapatana PS", "Dobbespet PS",

                "Doddaballapura Rural PS", "Doddaballapura Town PS", "Doddabelavangala PS", "Hebbagodi PS",

                "Hosahalli PS", "Hosakote PS", "Hosakote Traffic PS", "Jigani PS", "Madanayakanahally PS",

                "Nandagudi PS", "Nelamangala Rural PS", "Nelamangala Town PS", "Nelamangala Traffic PS",

                "Rajanukunte PS", "Sarjapura PS", "Sulibele PS", "Suryanagar PS", "Thirumalashettahalli PS",

                "Thyamagondlu PS", "Vijayapura PS", "Vishwanathapura PS", "DPO", "Computer Sec", "DAR",

                "FPB", "MCU", "DCRB", "DSB", "Social Media", "State INT", "DCRE", "Lokayukta", "ESCOM", "C/Room"

            )

            "Bidar -NR" -> listOf(

                "Aurad PS", "Bagdal PS", "Basava Kalyana Rural PS", "Basava Kalyana Town PS",

                "Basava Kalyana Traffic PS", "Bemalkhed PS", "Bhalki Rural PS", "Bhalki Town PS",

                "Bidar CEN Crime PS", "Bidar Rural PS", "Bidar Town PS", "Bidar Traffic PS", "Bidar Women PS",

                "Chintaki PS", "Chitaguppa PS", "Dhannura PS", "Gandhi Gunj PS", "Hallikhed PS", "Hokrana PS",

                "Hulsoor PS", "Humnabad PS", "Humnabad Traffic PS", "Janwada PS", "Kamalanagar PS",

                "Khataka Chincholi PS", "Kushnoor PS", "Manna Ekhelli PS", "Mannalli PS", "Manthala PS",

                "Market PS", "Mehakar PS", "Mudbi PS", "New Town PS", "Santhapur PS", "DPO", "Computer Sec",

                "DAR", "FPB", "MCU", "DCRB", "DSB", "Social Media", "State INT", "DCRE", "Lokayukta", "ESCOM", "C/Room"

            )

            "Chamarajanagar -SR" -> listOf(

                "Begur PS", "Chamarajanagar CEN Crime PS", "Chamarajanagar East PS", "Chamarajanagar Rural PS",

                "Chamarajanagar Town PS", "Chamarajanagar Traffic PS", "Chamrajanagar Women ps", "Gundlupete PS",

                "Hanur PS", "Kollegala Rural PS", "Kollegala Town PS", "Kuderu PS", "M.M. Hills PS",

                "Mambali PS", "Ramapura PS", "Santemaralli PS", "Terakanambi PS", "Yelandur PS", "DPO",

                "Computer Sec", "DAR", "FPB", "MCU", "DCRB", "DSB", "Social Media", "State INT", "DCRE",

                "Lokayukta", "ESCOM", "C/Room"

            )

            "Chikkaballapura -CR" -> listOf(

                "Bagepalli PS", "Batlahalli PS", "Chelur PS", "Chikkaballapura CEN Crime PS",

                "Chikkaballapura Rural PS", "Chikkaballapura Town PS", "Chikkaballapura Traffic PS",

                "Chikkaballapura Women PS", "Chintamani Rural PS", "Chintamani Town PS", "Dibburahalli PS",

                "Gowribidanur Rural PS", "Gowribidanur Town PS", "Gudibande PS", "Kencharlahalli PS",

                "Manchenalli PS", "Nandi Giridhama PS", "Patapalya PS", "Peresandra PS", "Shidlagatta Rural PS",

                "Shidlaghatta Town PS", "DPO", "Computer Sec", "DAR", "FPB", "MCU", "DCRB", "DSB",

                "Social Media", "State INT", "DCRE", "Lokayukta", "ESCOM", "C/Room"

            )

            "Chikkamagaluru -WR" -> listOf(

                "Ajjampura PS", "Aldur PS", "Balehonnur PS", "Balur PS", "Banakal PS", "Basavanahalli PS",

                "Birur PS", "Chickmagalur CEN Crime PS", "Chickmagalur Rural PS", "Chickmagalur Town PS",

                "Chickmagalur Traffic PS", "Chickmagalur Women PS", "Gonibeedu PS", "Hariharapura PS",

                "Jayapura PS", "Kadur PS", "Kalasa PS", "Koppa PS", "Kuduremukh PS", "Lakkavalli PS",

                "Lingadahalli PS", "Mallanduru PS", "Mudigere PS", "N.R. Pura PS", "Panchanahalli PS",

                "Sakkaraya Patna PS", "Singatagere PS", "Sringeri PS", "Tarikere Town PS", "Yagati PS",

                "DPO", "Computer Sec", "DAR", "FPB", "MCU", "DCRB", "DSB", "Social Media", "State INT",

                "DCRE", "Lokayukta", "ESCOM", "C/Room"

            )

            "Chitradurga -ER" -> listOf(

                "Abbinahole PS", "Bharamasagara PS", "Challakere PS", "Chikkajajur PS", "Chitradurga CEN Crime PS",

                "Chitradurga Extension PS", "Chitradurga Kote PS", "Chitradurga Rural PS", "Chitradurga Town PS",

                "Chitradurga Traffic PS", "Chitradurga Women PS", "Chitrahalli Gate PS", "Hiriyur Rural PS",

                "Hiriyur Town PS", "Holalkere PS", "Hosadurga PS", "Imangala PS", "Molakalmuru PS",

                "Nayakanhatti PS", "Parasurampura PS", "Rampura PS", "Srirampura PS", "Thalak PS",

                "Turuvanur (Jogi) PS", "DPO", "Computer Sec", "DAR", "FPB", "MCU", "DCRB", "DSB",

                "Social Media", "State INT", "DCRE", "Lokayukta", "ESCOM", "C/Room"

            )

            "Dakshina Kannada -WR" -> listOf(

                "Bantwala Rural PS", "Bantwala Town PS", "Bantwala Traffic PS", "Bellare PS", "Belthangadi PS",

                "Belthangadi Traffic PS", "Dharmasthala PS", "DK CEN Crime PS", "DK Women PS", "Kadaba PS",

                "Punjalkatte PS", "Puttur Rural PS", "Puttur Town PS", "Puttur Traffic PS", "Subramanya PS",

                "Sullia PS", "Uppinangadi PS", "Venoor PS", "Vitla PS", "DPO", "Computer Sec", "DAR",

                "FPB", "MCU", "DCRB", "DSB", "Social Media", "State INT", "DCRE", "Lokayukta", "ESCOM", "C/Room"

            )

            "Davanagere -ER" -> listOf(

                "Azad Nagar PS", "Basavanagara PS", "Basavapatna PS", "Bilichodu PS", "Channagiri PS",

                "Davanagere CEN Crime PS", "Davanagere Extention PS", "Davanagere Rural PS",

                "Davanagere South Traffic PS", "Davanagere Women PS", "Davangere North Traffic PS",

                "Gandhi Nagar PS", "Hadadi PS", "Harihara Rural PS", "Harihara Town PS", "Honnali PS",

                "Jagalur PS", "KTJ Nagar PS", "Malebennur PS", "Mayakonda PS", "Nyamathi PS", "RMC Yard PS",

                "Santhebennur PS", "Vidyanagar PS", "DPO", "Computer Sec", "DAR", "FPB", "MCU", "DCRB",

                "DSB", "Social Media", "State INT", "DCRE", "Lokayukta", "ESCOM", "C/Room"

            )

            "Dharwad -NR" -> listOf(

                "Alnavar PS", "Annigeri PS", "Dharwad CEN Crime PS", "Dharwad Rural PS", "Dharwad Women PS",

                "Garag PS", "Gudageri PS", "Hubli Rural PS", "Kalaghatagi PS", "Kundagol PS", "Navalgund PS",

                "DPO", "Computer Sec", "DAR", "FPB", "MCU", "DCRB", "DSB", "Social Media", "State INT",

                "DCRE", "Lokayukta", "ESCOM", "C/Room"

            )

            "Gadag -NR" -> listOf(

                "Betageri PS", "Betageri Extention PS", "Gadag CEN Crime PS", "Gadag Rural PS", "Gadag Town PS",

                "Gadag Traffic PS", "Gadag Women PS", "Gajendragad PS", "Lakshmeshwar PS", "Mulagund PS",

                "Mundargi PS", "Naregal PS", "Nargund PS", "Ron PS", "Shirahatti PS", "DPO", "Computer Sec",

                "DAR", "FPB", "MCU", "DCRB", "DSB", "Social Media", "State INT", "DCRE", "Lokayukta", "ESCOM", "C/Room"

            )

            "Hassan -SR" -> listOf(

                "Alur PS", "Arakalagud PS", "Arasikere Rural PS", "Arasikere Town PS", "Arehalli PS",

                "Banavara PS", "Belur PS", "Channarayapatna Rural PS", "Channarayapatna Town PS",

                "Channarayapatna Traffic PS", "Dudda PS", "Gandasi PS", "Goruru PS", "Halebeedu PS",

                "Hallymysore PS", "Hassan CEN Crime PS", "Hassan City PS", "Hassan Extn. PS", "Hassan Rural PS",

                "Hassan Traffic PS", "Hassan Women PS", "Hirisave PS", "Holenarasipura Rural PS",

                "Holenarasipura Town PS", "Javagal PS", "Konanur PS", "Nuggehalli PS", "Pension Mohalla PS",

                "Sakaleshpura Rural PS", "Sakaleshpura Town PS", "Shanthigrama PS", "Shravanabelagola PS",

                "Yeslur PS", "DPO", "Computer Sec", "DAR", "FPB", "MCU", "DCRB", "DSB", "Social Media",

                "State INT", "DCRE", "Lokayukta", "ESCOM", "C/Room"

            )

            "Haveri -ER" -> listOf(

                "Adur PS", "Bankapura PS", "Byadagi PS", "Guttal PS", "Halageri PS", "Hanagal PS",

                "Hansabhavi PS", "Haveri CEN Crime PS", "Haveri Rural PS", "Haveri Town PS", "Haveri Traffic PS",

                "Haveri Women PS", "Hirekerur PS", "Hulagur PS", "Kaginele PS", "Kumarapattanam PS",

                "Ranebennur Rural PS", "Ranebennur Town PS", "Ranebennur Traffic PS", "Rattihalli PS",

                "Savanoor PS", "Shiggaon PS", "Tadas PS", "DPO", "Computer Sec", "DAR", "FPB", "MCU",

                "DCRB", "DSB", "Social Media", "State INT", "DCRE", "Lokayukta", "ESCOM", "C/Room"

            )

            "Hubballi Dharwad City -COP" -> listOf(

                "APMC Navanagar", "Ashoknagar PS", "Bendigeri PS", "CEN Crime PS Hubballi Dharwad City",

                "Dharwad Sub-Urban PS", "Dharwad Town PS", "Dharwad Traffic PS", "E and N Crime PS HD city",

                "Ghantikeri PS", "Gokul Road PS", "HD City Women PS", "Hubballi East Traffic PS",

                "Hubballi North Traffic PS", "Hubballi South Traffic PS", "Hubballi Sub Urban PS",

                "Hubballi Town PS", "Kamaripeth PS", "Kasabapeth PS", "Keshavapur PS", "Old Hubballi PS",

                "Vidyagiri PS", "Vidyanagar PS", "DPO", "Computer Sec", "CAR", "FPB", "MCU", "CCRB", "CSB",

                "Social Media", "State INT", "DCRE", "Lokayukta", "ESCOM", "C/Room"

            )

            "Kalaburagi -NER" -> listOf(

                "Afzalpur PS", "Alland PS", "Chincholi PS", "Chittapura PS", "Devalagangapur PS", "Jewargi PS",

                "Kalaburagi CEN Crime PS", "Kalaburagi Women PS", "Kalagi PS", "Kamalapur PS", "Kunchavaram PS",

                "Kurakunta PS", "Madanahipparga PS", "Madbool PS", "Mahagoan PS", "Malkhed PS", "Miryan PS",

                "Mudhol PS", "Narona PS", "Nelogi PS", "Nimbarga PS", "Ratkal PS", "Revoor PS", "Sedam PS",

                "Shahabad Town PS", "Sulepet PS", "Wadi PS", "Yedrami PS", "DPO", "Computer Sec", "DAR",

                "FPB", "MCU", "DCRB", "DSB", "Social Media", "State INT", "DCRE", "Lokayukta", "ESCOM", "C/Room"

            )

            "Kalaburagi City -COP" -> listOf(

                "Ashoknagar PS", "Brahmapur PS", "Chowk PS", "Ferhatabad PS", "Kalaburagi City CENCrime PS",

                "Kalaburagi City Women PS", "Kalaburagi Traffic I PS", "Kalaburagi Traffic II PS", "MB Nagar PS",

                "Ragavendranagar PS", "Roza PS", "Station Bazar PS", "Sub Urban PS", "University PS", "DPO",

                "Computer Sec", "CAR", "FPB", "MCU", "CCRB", "CSB", "Social Media", "State INT", "DCRE",

                "Lokayukta", "ESCOM", "C/Room"

            )

            "KGF -CR" -> listOf(

                "Andersonpet PS", "Bangarpet PS", "BEML Nagar PS", "Bethamangala PS", "Budikote PS",

                "Champion Reefs PS", "Kamasamudram PS", "KGF CEN Crime PS", "Kyasamballi PS", "Marikuppam PS",

                "Oorgaum PS", "Robertsonpet PS", "DPO", "Computer Sec", "DAR", "FPB", "MCU", "DCRB",

                "DSB", "Social Media", "State INT", "DCRE", "Lokayukta", "ESCOM", "C/Room"

            )

            "Kodagu -SR" -> listOf(

                "Bhagamandala PS", "Gonikoppa PS", "Kodagu CEN Crime PS", "Kodagu Women PS",

                "Kushalanagar Rural PS", "Kushalnagar Town PS", "Kushalnagar Traffic PS", "Kutta PS",

                "Madikeri Rural PS", "Madikeri Town PS", "Madikeri Traffic PS", "Napoklu PS", "Ponnampet PS",

                "Shanivarasanthe PS", "Siddapura PS", "Somwarpet PS", "Srimangala PS", "Sunticoppa PS",

                "Virajpet Rural PS", "Virajpet Town PS", "DPO", "Computer Sec", "DAR", "FPB", "MCU",

                "DCRB", "DSB", "Social Media", "State INT", "DCRE", "Lokayukta", "ESCOM", "C/Room"

            )

            "Kolar -CR" -> listOf(

                "Gownapalli PS", "Gulpet PS", "Kolar CEN Crime PS", "Kolar Rural PS", "Kolar Town PS",

                "Kolar Traffic PS", "Kolar Women PS", "Malur PS", "Masti PS", "Mulbagal Rural PS",

                "Mulbagal Town PS", "Nangli PS", "Rayalpad PS", "Srinivasapura PS", "Vemagal PS", "DPO",

                "Computer Sec", "DAR", "FPB", "MCU", "DCRB", "DSB", "Social Media", "State INT", "DCRE",

                "Lokayukta", "ESCOM", "C/Room"

            )

            "Koppal -BR" -> listOf(

                "Alwandi PS", "Bevoor PS", "Gangavathi Rural PS", "Gangavathi Town PS", "Gangavathi Traffic PS",

                "Hanumasagar PS", "Kanakagiri PS", "Karatagi PS", "Koppal CEN Crime PS", "Koppal Rural PS",

                "Koppal Town PS", "Koppal Traffic PS", "Koppal Women PS", "Kuknoor PS", "Kushtagi PS",

                "Munirabad PS", "Tavaregera PS", "Yelburga PS", "DPO", "Computer Sec", "DAR", "FPB", "MCU",

                "DCRB", "DSB", "Social Media", "State INT", "DCRE", "Lokayukta", "ESCOM", "C/Room"

            )

            "Mandya -SR" -> listOf(

                "Arakere PS", "Basaralu PS", "Belakavadi PS", "Bellur PS", "Besagaraghalli PS", "Bindiganavile PS",

                "Halagur PS", "K.M. Doddi PS", "K.R. Pet Rural PS", "K.R. Pet Town PS", "K.R. Sagar PS",

                "Keragodu PS", "Kesthur PS", "Kikkeri PS", "Kirugavalu PS", "Koppa PS", "Maddur PS",

                "Maddur Traffic PS", "Malavalli Rural PS", "Malavalli Town PS", "Mandya CEN Crime PS",

                "Mandya Central PS", "Mandya East PS", "Mandya Rural PS", "Mandya Traffic PS", "Mandya West PS",

                "Mandya Women PS", "Melukote PS", "Nagamangala Rural PS", "Nagamangala Town PS", "Pandavapura PS",

                "Shivalli PS", "Srirangapatna PS", "Srirangapatna Rural PS", "DPO", "Computer Sec", "DAR",

                "FPB", "MCU", "DCRB", "DSB", "Social Media", "State INT", "DCRE", "Lokayukta", "ESCOM", "C/Room"

            )

            "Mangaluru City -COP" -> listOf(

                "Bajpe PS", "Barke PS", "CEN Crime PS Mangaluru City", "E and N Crime PS Mangaluru City",

                "Kankanady Town PS", "Kavoor PS", "Mangalore East PS", "Mangalore East Traffic PS",

                "Mangalore North PS", "Mangalore Rural PS", "Mangalore South PS", "Mangalore West Traffic PS",

                "Mangalore Women PS", "Moodabidre PS", "Mulki PS", "Surathkal PS", "Traffic North Police Station",

                "Traffic South Police Station", "Ullal PS", "Urva PS", "DPO", "Computer Sec", "CAR", "FPB",

                "MCU", "CCRB", "CSB", "Social Media", "State INT", "DCRE", "Lokayukta", "ESCOM", "C/Room"

            )

            "Mysuru City -COP" -> listOf(

                "Alanahally PS", "Ashokpuram PS", "CEN Crime PS Mysuru City", "Devaraja PS", "Deveraja Traffic PS",

                "E and N Crime PS Mysuru City", "Hebbal PS", "Jayalakshmipuram PS", "Krishnaraja PS",

                "Krishnaraja Traffic PS", "Kuvempunagar PS", "Lashkar PS", "Laxmipuram PS", "Mandi PS",

                "Metagalli PS", "Mysuru City Women PS", "Narasimharaja PS", "Narasimharaja Traffic PS",

                "Nazarbad PS", "Saraswathipuram PS", "Siddarthanagar Traffic PS", "Udayagiri PS",

                "V V Puram Traffic PS", "V.V. Puram PS", "Vidyaranyarpuram PS", "Vijayanagar PS", "DPO",

                "Computer Sec", "CAR", "FPB", "MCU", "CCRB", "CSB", "Social Media", "State INT", "DCRE",

                "Lokayukta", "ESCOM", "C/Room"

            )

            "Mysuru Dist -SR" -> listOf(

                "Bannur PS", "Beechanahalli PS", "Bettadapura PS", "Biligere PS", "Bilikere PS", "Bylakuppe PS",

                "H.D. Kote PS", "Hullahalli PS", "Hunusur Rural PS", "Hunusur Town PS", "Jayapura PS",

                "K.R. Nagar PS", "Kowlande PS", "Mysuru CEN Crime PS", "Mysuru South PS", "Mysuru Women PS",

                "Nanjanagudu Rural PS", "Nanjanagudu Town PS", "Nanjangudu Traffic PS", "Periyapatna PS",

                "Saligrama PS", "Saragur PS", "T. Narasipura PS", "Talakadu PS", "Varuna PS", "Yelawala PS",

                "DPO", "Computer Sec", "DAR", "FPB", "MCU", "DCRB", "DSB", "Social Media", "State INT",

                "DCRE", "Lokayukta", "ESCOM", "C/Room"

            )

            "Raichur -BR" -> listOf(

                "Balaganoor PS", "Devadurga PS", "Devadurga Traffic PS", "Gabbur PS", "Hutti PS", "Idapanur PS",

                "Jalhalli PS", "Kowthal PS", "Lingasugur PS", "Manvi PS", "Market Yard PS", "Maski PS", "Mudgal PS",

                "Netajinagar PS", "Raichur CEN Crime PS", "Raichur Rural PS", "Raichur Traffic PS", "Raichur West PS",

                "Raichur Women PS", "Sadar Bazar PS", "Shakti Nagar PS", "Sindanoor Rural PS", "Sindanur Traffic PS",

                "Sindhanoor Town PS", "Sirwar PS", "Turvihal PS", "Yapaladinni PS", "Yeregera PS", "DPO",

                "Computer Sec", "DAR", "FPB", "MCU", "DCRB", "DSB", "Social Media", "State INT", "DCRE",

                "Lokayukta", "ESCOM", "C/Room"

            )

            "Ramanagara -CR" -> listOf(

                "Akkur PS", "Bidadi PS", "Channapatna East PS", "Channapatna Rural PS", "Channapatna Town PS",

                "Channapatna Traffic PS", "Harohalli PS", "Ijoor PS", "Kaggalipura PS", "Kanakapura Rural PS",

                "Kanakapura Town PS", "Kanakapura Traffic PS", "Kodihalli PS", "Kudur PS", "Kumbalagudu PS",

                "M.K. Doddi PS", "Magadi PS", "Ramanagara CEN Crime PS", "Ramanagara Rural PS", "Ramanagara Town PS",

                "Ramanagara Traffic PS", "Ramanagara Women PS", "Sathanoor PS", "Tavarekere PS", "DPO",

                "Computer Sec", "DAR", "FPB", "MCU", "DCRB", "DSB", "Social Media", "State INT", "DCRE",

                "Lokayukta", "ESCOM", "C/Room"

            )

            "Shivamogga -ER" -> listOf(

                "Agumbe PS", "Anandapura PS", "Anavatti PS", "Bhadravathi New Town PS", "Bhadravathi Old Town PS",

                "Bhadravathi Rural PS", "Bhadravathi Traffic PS", "Doddapete PS", "Holehonnur PS", "Hosamane PS",

                "Hosanagara PS", "Jayanagara PS", "Kargal PS", "Kote PS", "Kumsi PS", "Malur PS", "Nagara PS",

                "Paper Town PS", "Ripponpet PS", "Sagar Rural PS", "Sagar Town PS", "Shikaripura Rural PS",

                "Shikaripura Town PS", "Shiralakoppa PS", "Shivamogga CEN Crime PS", "Shivamogga East Traffic PS",

                "Shivamogga Rural PS", "Shivamogga West Traffic PS", "Shivamogga Women PS", "Soraba PS",

                "Thirthahalli PS", "Tunga Nagar PS", "Vinobanagara PS", "DPO", "Computer Sec", "DAR", "FPB",

                "MCU", "DCRB", "DSB", "Social Media", "State INT", "DCRE", "Lokayukta", "ESCOM", "C/Room"

            )

            "Tumakuru -CR" -> listOf(

                "Amruthur PS", "Arasikere PS", "Badavanahalli PS", "Bellavi PS", "Chandrashekarpura PS",

                "Chelur PS", "Chikkanayakanahalli PS", "Dandina Shivara PS", "Gubbi PS", "Handanakere PS",

                "Hebbur PS", "Honnavalli PS", "Huliyar PS", "Huliyurdurga PS", "Jayanagara PS", "Kallambella PS",

                "Kibbanhalli PS", "Kodigenahalli PS", "Kolala PS", "Kora PS", "Koratagere PS", "Kunigal PS",

                "Kyathasandra PS", "Madhugiri PS", "Medigeshi PS", "New Extention PS", "Nonavinakere PS",

                "Patanayakanahalli PS", "Pavagada PS", "Sira PS", "Tavarekere PS", "Thilak Park PS",

                "Thirumani PS", "Tiptur Rural PS", "Tiptur Town PS", "Tumakuru CEN Crime PS", "Tumakuru Rural PS",

                "Tumakuru Town PS", "Tumakuru Traffic PS", "Tumakuru Women PS", "Turuvekere PS", "Y.N. Hosakote PS",

                "DPO", "Computer Sec", "DAR", "FPB", "MCU", "DCRB", "DSB", "Social Media", "State INT",

                "DCRE", "Lokayukta", "ESCOM", "C/Room"

            )

            "Udupi -WR" -> listOf(

                "Ajekar PS", "Amasebailu PS", "Brahmavar PS", "Byndoor PS", "Gangolli PS", "Hebri PS",

                "Hiriadka PS", "Kapu PS", "Karkala Rural PS", "Karkala Town PS", "Kollur PS", "Kota PS",

                "Kundapura PS", "Kundapura Rural PS", "Kundapura Traffic PS", "Malpe PS", "Manipal PS",

                "Padubidri PS", "Shankaranarayana PS", "Shirva PS", "Udupi CEN Crime PS", "Udupi Town PS",

                "Udupi Traffic PS", "Udupi Women PS", "DPO", "Computer Sec", "DAR", "FPB", "MCU", "DCRB",

                "DSB", "Social Media", "State INT", "DCRE", "Lokayukta", "ESCOM", "C/Room"

            )

            "Uttara Kannada -WR" -> listOf(

                "Ambikanagar PS", "Ankola PS", "Banavasi PS", "Bhatkal Rural PS", "Bhatkal Town PS",

                "Chittakula PS", "Dandeli Rural PS", "Dandeli Town PS", "Gokarna PS", "Haliyal PS",

                "Honnavara PS", "Joida PS", "Kadra PS", "Karwar Railway PS", "Karwar Rural PS", "Karwar Town PS",

                "Karwar Traffic PS", "Kumta PS", "Mallapura PS", "Manki PS", "Mundgod PS", "Murudeshwar PS",

                "Ramanagar PS", "Siddapura PS", "Sirsi New Market PS", "Sirsi Rural PS", "Sirsi Town PS",

                "UK CEN Crime PS", "UK Women PS", "Yellapura PS", "DPO", "Computer Sec", "DAR", "FPB",

                "MCU", "DCRB", "DSB", "Social Media", "State INT", "DCRE", "Lokayukta", "ESCOM", "C/Room"

            )

            "Vijayanagara -BR" -> listOf(

                "Arasikere PS", "Chigateri PS", "Chittavadagi PS", "Gudekote PS", "Hadagali PS",

                "Hagaribommanahalli PS", "Halavagilu PS", "Hampi Tourism PS", "Hampi Traffic PS",

                "Harapanahalli PS", "Hirehadagali PS", "Hosahalli PS", "Hospet Extention PS", "Hospet Rural PS",

                "Hospet Town PS", "Hospet Traffic PS", "Ittigi PS", "Kamalapur PS", "Kottur PS", "Kudligi PS",

                "Mariyammanahalli PS", "T.B. Dam PS", "T.B. Halli PS", "DPO", "Computer Sec", "DAR",

                "FPB", "MCU", "DCRB", "DSB", "Social Media", "State INT", "DCRE", "Lokayukta", "ESCOM", "C/Room"

            )

            "Vijayapura -NR" -> listOf(

                "Adarsh Nagar PS", "Alamatti PS", "Almel PS", "APMC PS", "Babaleshwar PS", "Basavan Bagewadi PS",

                "Chadachan PS", "Devara Hipparagi PS", "Gandhi Chowk PS", "Golgumbaz PS", "Hortti PS", "Indi PS",

                "Indi Rural PS", "Jalanagar PS", "Kalkeri PS", "Kolhar PS", "Kudagi PS", "Managuli PS",

                "Muddebihal PS", "Nidagundi PS", "Sindagi PS", "Talikot PS", "Tikota PS", "Vijayapura CEN Crime PS",

                "Vijayapura Rural PS", "Vijayapura Traffic PS", "Vijayapura Women PS", "Zalaki PS", "DPO",

                "Computer Sec", "DAR", "FPB", "MCU", "DCRB", "DSB", "Social Media", "State INT", "DCRE",

                "Lokayukta", "ESCOM", "C/Room"

            )

            "Yadgir -NER" -> listOf(

                "Bheemarayanagudi PS", "Gogi PS", "Gurumitkal PS", "Hunasagi PS", "Kembhavi PS", "Kodekal PS",

                "Narayanapura PS", "Saidapur PS", "Shahapur PS", "Shorapur PS", "Wadigere PS",

                "Yadgiri CEN Crime PS", "Yadgiri Rural PS", "Yadgiri Town PS", "Yadgiri Traffic PS",

                "Yadgiri Women PS", "DPO", "Computer Sec", "DAR", "FPB", "MCU", "DCRB", "DSB",

                "Social Media", "State INT", "DCRE", "Lokayukta", "ESCOM", "C/Room"

            )

            else -> emptyList()

        }



        // Final Combine: District Header + Sorted Hardcoded List

        (listOf(districtFullName) + stations.distinct().sorted()).distinct()

    }

}