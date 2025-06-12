package com.example.lab6_20206331.models;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class User implements Parcelable {

    private String uid;
    private String email;
    private String displayName;
    private String photoUrl;
    private String phoneNumber;
    private Date createdAt;
    private Date lastSignIn;
    private String loginProvider; // "email", "google", "facebook"
    private boolean isEmailVerified;
    private Map<String, Object> preferences;

    // Constructor vacío (requerido por Firebase)
    public User() {
        this.preferences = new HashMap<>();
    }

    // Constructor básico
    public User(String uid, String email) {
        this.uid = uid;
        this.email = email;
        this.createdAt = new Date();
        this.lastSignIn = new Date();
        this.preferences = new HashMap<>();
        this.loginProvider = "email";
        this.isEmailVerified = false;
    }

    // Constructor completo
    public User(String uid, String email, String displayName, String photoUrl, String loginProvider) {
        this.uid = uid;
        this.email = email;
        this.displayName = displayName;
        this.photoUrl = photoUrl;
        this.loginProvider = loginProvider;
        this.createdAt = new Date();
        this.lastSignIn = new Date();
        this.preferences = new HashMap<>();
        this.isEmailVerified = false;
    }

    // Getters y Setters
    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getLastSignIn() {
        return lastSignIn;
    }

    public void setLastSignIn(Date lastSignIn) {
        this.lastSignIn = lastSignIn;
    }

    public String getLoginProvider() {
        return loginProvider;
    }

    public void setLoginProvider(String loginProvider) {
        this.loginProvider = loginProvider;
    }

    public boolean isEmailVerified() {
        return isEmailVerified;
    }

    public void setEmailVerified(boolean emailVerified) {
        isEmailVerified = emailVerified;
    }

    public Map<String, Object> getPreferences() {
        return preferences;
    }

    public void setPreferences(Map<String, Object> preferences) {
        this.preferences = preferences;
    }

    // Métodos utilitarios
    public String getDisplayNameOrEmail() {
        if (displayName != null && !displayName.trim().isEmpty()) {
            return displayName;
        } else if (email != null) {
            // Extraer nombre del email (antes del @)
            return email.substring(0, email.indexOf("@"));
        }
        return "Usuario";
    }

    public boolean hasProfilePicture() {
        return photoUrl != null && !photoUrl.trim().isEmpty();
    }

    public void updateLastSignIn() {
        this.lastSignIn = new Date();
    }

    // Método para convertir a Map para Firestore
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("uid", uid);
        map.put("email", email);
        map.put("displayName", displayName);
        map.put("photoUrl", photoUrl);
        map.put("phoneNumber", phoneNumber);
        map.put("createdAt", createdAt);
        map.put("lastSignIn", lastSignIn);
        map.put("loginProvider", loginProvider);
        map.put("isEmailVerified", isEmailVerified);
        map.put("preferences", preferences);
        return map;
    }

    // Método para crear desde Map de Firestore
    public static User fromMap(String uid, Map<String, Object> map) {
        User user = new User();
        user.setUid(uid);
        user.setEmail((String) map.get("email"));
        user.setDisplayName((String) map.get("displayName"));
        user.setPhotoUrl((String) map.get("photoUrl"));
        user.setPhoneNumber((String) map.get("phoneNumber"));

        // Manejar fechas
        Object createdAt = map.get("createdAt");
        if (createdAt instanceof Date) {
            user.setCreatedAt((Date) createdAt);
        } else if (createdAt instanceof Long) {
            user.setCreatedAt(new Date((Long) createdAt));
        }

        Object lastSignIn = map.get("lastSignIn");
        if (lastSignIn instanceof Date) {
            user.setLastSignIn((Date) lastSignIn);
        } else if (lastSignIn instanceof Long) {
            user.setLastSignIn(new Date((Long) lastSignIn));
        }

        user.setLoginProvider((String) map.get("loginProvider"));

        Object emailVerified = map.get("isEmailVerified");
        if (emailVerified instanceof Boolean) {
            user.setEmailVerified((Boolean) emailVerified);
        }

        Object preferences = map.get("preferences");
        if (preferences instanceof Map) {
            user.setPreferences((Map<String, Object>) preferences);
        }

        return user;
    }

    // Parcelable implementation
    protected User(Parcel in) {
        uid = in.readString();
        email = in.readString();
        displayName = in.readString();
        photoUrl = in.readString();
        phoneNumber = in.readString();
        createdAt = new Date(in.readLong());
        lastSignIn = new Date(in.readLong());
        loginProvider = in.readString();
        isEmailVerified = in.readByte() != 0;
        preferences = new HashMap<>();
        in.readMap(preferences, getClass().getClassLoader());
    }

    public static final Creator<User> CREATOR = new Creator<User>() {
        @Override
        public User createFromParcel(Parcel in) {
            return new User(in);
        }

        @Override
        public User[] newArray(int size) {
            return new User[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(uid);
        dest.writeString(email);
        dest.writeString(displayName);
        dest.writeString(photoUrl);
        dest.writeString(phoneNumber);
        dest.writeLong(createdAt != null ? createdAt.getTime() : 0);
        dest.writeLong(lastSignIn != null ? lastSignIn.getTime() : 0);
        dest.writeString(loginProvider);
        dest.writeByte((byte) (isEmailVerified ? 1 : 0));
        dest.writeMap(preferences);
    }

    @Override
    public String toString() {
        return "User{" +
                "uid='" + uid + '\'' +
                ", email='" + email + '\'' +
                ", displayName='" + displayName + '\'' +
                ", loginProvider='" + loginProvider + '\'' +
                ", isEmailVerified=" + isEmailVerified +
                '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        User user = (User) obj;
        return uid != null ? uid.equals(user.uid) : user.uid == null;
    }

    @Override
    public int hashCode() {
        return uid != null ? uid.hashCode() : 0;
    }
}