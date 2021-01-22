package com.demo.smileid.sid_sdk.sidNet;

import com.smileidentity.libsmileid.core.idcard.Country;
import com.smileidentity.libsmileid.core.idcard.IdCard;
import com.smileidentity.libsmileid.core.idcard.IdType;

public class IdTypeUtil {

    public static IdCard idCards(String country) {
        switch (country) {
            case Country.GHANA:
                return new IdCard.For(Country.GHANA)
                        .setCountryCode("233")
                        .addCard(IdType.NATIONAL_ID.replace("_", " "))
                        .addCard(IdType.PASSPORT)
                        .addCard(IdType.SSNIT)
                        .addCard(IdType.VOTER_ID.replace("_", " "))
                        .addCard(IdType.DRIVER_LICENSE.replace("_", " "))
                        .addCard(IdType.OTHER_ID_TYPE.replace("_", " "))
                        .create();

            case Country.KENYA:
                return new IdCard.For(Country.KENYA)
                        .setCountryCode("254")
                        .addCard(IdType.NATIONAL_ID.replace("_", " "))
                        .addCard(IdType.PASSPORT)
                        .addCard(IdType.ALIEN_CARD.replace("_", " "))
                        .addCard(IdType.OTHER_ID_TYPE.replace("_", " "))
                        .addCard(IdType.REFUGEE_CARD.replace("_", " "))
                        .create();

            case Country.NIGERIA:
                return new IdCard.For(Country.NIGERIA)
                        .setCountryCode("234")
                        .addCard(IdType.DRIVER_LICENSE.replace("_", " "))
                        .addCard(IdType.NATIONAL_ID.replace("_", " "))
                        .addCard(IdType.PASSPORT)
                        .addCard(IdType.BVN)
                        .addCard(IdType.NIN)
                        .addCard(IdType.VOTER_ID.replace("_", " "))
                        .addCard(IdType.OTHER_ID_TYPE.replace("_", " "))
                        .create();

            case Country.TANZANIA:
                return new IdCard.For(Country.TANZANIA)
                        .setCountryCode("255")
                        .addCard(IdType.DRIVER_LICENSE.replace("_", " "))
                        .addCard(IdType.PASSPORT)
                        .addCard(IdType.CITIZEN_ID.replace("_", " "))
                        .addCard(IdType.VOTER_ID.replace("_", " "))
                        .addCard(IdType.STUDENT_ID.replace("_", " "))
                        .addCard(IdType.NATIONAL_ID.replace("_", " "))
                        .addCard(IdType.OTHER_ID_TYPE.replace("_", " "))
                        .create();

            default:
                return new IdCard.For(Country.STATELESS)
                        .addCard(IdType.OTHER_ID_TYPE.replace("_", " "))
                        .create();
        }
    }
}
