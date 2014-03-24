package com.foursevengames.speaknews; 
 
public class Feed {
 
    //private variables
    int _id = 0;
    String _name = "";
    String _rss_url = "";
 
    // Empty constructor
    public Feed(){
 
    }
    // constructor
    public Feed(int id, String name, String _rss_url){
        this._id = id;
        this._name = name;
        this._rss_url = _rss_url;
    }
 
    // constructor
    public Feed(String name, String _rss_url){
        this._name = name;
        this._rss_url = _rss_url;
    }
    // getting ID
    public int getID(){
        return this._id;
    }
 
    // setting id
    public void setID(int id){
        this._id = id;
    }
 
    // getting name
    public String getName(){
        return this._name;
    }
 
    // setting name
    public void setName(String name){
        this._name = name;
    }
 
    // getting phone number
    public String getRssUrl(){
        return this._rss_url;
    }
 
    // setting phone number
    public void setRssUrl(String rss_url){
        this._rss_url = rss_url;
    }
}
