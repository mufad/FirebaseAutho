package com.droiddigger.firebaseautho;

import com.google.firebase.database.Exclude;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by mufad on 8/15/2016.
 */
public class Joke {
    //User Id of the logged in User
    public String uid;
    //Joke Text
    public String text;
    //User Name of the logged in User
    public String name;
    //Display Picture of the logged in User
    public String photoUrl;
    //Firebase assigns a unique key to every Joke posted
    public String jokeKey;
    //Total count of the likes for this Joke
    public int likeCount = 0;
    //Map to store the user ids of the User who has liked this joke
    public Map<String, Boolean> likesGivenBy = new HashMap<>();


    public Joke() {
    }

    public Joke(String uid, String text, String name, String photoUrl, String jokeKey, Map likesGivenBy) {
        this.uid = uid;
        this.text = text;
        this.name = name;
        this.photoUrl = photoUrl;
        this.jokeKey = jokeKey;
        this.likesGivenBy = likesGivenBy;
    }

    @Exclude
    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("uid", uid);
        result.put("name", name);
        result.put("text", text);
        result.put("photoUrl", photoUrl);
        result.put("jokeKey", jokeKey);
        result.put("likeCount", likeCount);
        result.put("likesGivenBy", likesGivenBy);

        return result;
    }

}
