package com.semibit.ezandroidutils.models;


import android.widget.ImageView;

import androidx.annotation.Nullable;

import com.semibit.ezandroidutils.App;
import  com.semibit.ezandroidutils.Constants;
import  com.semibit.ezandroidutils.R;
import  com.semibit.ezandroidutils.services.LoginService;
import  com.semibit.ezandroidutils.EzUtils;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Created by shivesh on 29/6/17.
 */

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class GenricUser {

    private String name;
    private String alias;
    private String password ;
    private String id;
    private String email;
    private String image;
    private String avatar;
    private String phone;
    private String gender ="male";
    private String type;
    private String dob;
    private String about;

    private String rank;

    private String webIdToken;
    private boolean isAnonymous=true;

    private String fcmToken;
    private String status;
    private String verifdoc;
    private Float weeklyAward;

    public String getName() {
        return ""+name;
    }

    public String getAliasAndVerif() {
        String status=this.getStatus();
        if(status.startsWith("VERIFIED"))
            return alias+ Constants.V2V_VERIFIED;
        return alias;
    }
    public void setGender(String gender) {
        this.gender = gender.toLowerCase();
    }

    @Nullable
    public int getAge() {

        if(dob ==null || dob.length()<5)
            return 0;

        Date d=new Date(Long.parseLong(dob));
        int age= Calendar.getInstance().getTime().getYear()-
                d.getYear();

        return  age;

    }


    @Nullable
    public int getAgeInt() {

        if(dob ==null )
            return 0;

        Date d=new Date(Long.parseLong(dob));
        int age= Calendar.getInstance().getTime().getYear()-
                d.getYear();

        return  age;

    }

    public String  getDateofbirthString() {
        SimpleDateFormat formatter = new SimpleDateFormat("dd MMMM yyyy");
        if(getDob()==null)
        return null;
        Date date = new Date(Long.parseLong(getDob()));
        return formatter.format(date);
    }

    public String getDob() {
        return dob;
    }

    public void setDob(String dob) {
        this.dob = dob;
    }


    public boolean canPost(){
        return getStatus()!=null && (isAdmin() ||getStatus().equals(Constants.userStatuses[2]) );
    }
    public boolean isAdmin(){
        return getStatus()!=null && getStatus().equals(Constants.userStatuses[3]);
    }

    public String getStatus() {
        if(status==null) status="";
        return status;
    }
    public static void renderImage(String userImUrl,ImageView img){

        if(userImUrl!=null && userImUrl.startsWith("http")){
            Picasso.get().load(userImUrl)
                    .error(R.drawable.ic_users).into(img);

        }
    }

    public String hisHer() {
        return getGender().replace("female","her").replace("male","his");
    }


    public boolean validate(){
        return !EzUtils.isEmpty(phone) &&
                (!EzUtils.isEmpty(dob) || App.getInstance().getBoolean("disable_age_check")) &&
                !EzUtils.isEmpty(email) &&
                !EzUtils.isEmpty(name) &&
                (!LoginService.isPasswordMandatoryForSignup() || !EzUtils.isEmpty(password)) ;
    }

    public String getImage(){
        return image;
    }

}
