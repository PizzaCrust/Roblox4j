package online.pizzacrust.roblox.impl;

import com.google.gson.Gson;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import online.pizzacrust.roblox.Asset;
import online.pizzacrust.roblox.Badge;
import online.pizzacrust.roblox.CachedGroupData;
import online.pizzacrust.roblox.ClubType;
import online.pizzacrust.roblox.Place;
import online.pizzacrust.roblox.Presence;
import online.pizzacrust.roblox.Roblox;
import online.pizzacrust.roblox.Robloxian;
import online.pizzacrust.roblox.auth.AuthenticationInfo;
import online.pizzacrust.roblox.errors.InvalidUserException;
import online.pizzacrust.roblox.group.Group;

public class BasicRobloxian extends BasicProfile implements Robloxian {
    private final Document document;

    public BasicRobloxian(String username) throws InvalidUserException {
        super(username);
        try {
            enableSSLSocket();
            document = Jsoup.connect(getProfileUrl()).ignoreContentType(true).get();
        } catch (Exception e) {
            e.printStackTrace();
            throw new InvalidUserException();
        }
    }

    public BasicRobloxian(int id) throws InvalidUserException {
        super(id);
        try {
            enableSSLSocket();
            document = Jsoup.connect(getProfileUrl()).ignoreContentType(true).get();
        } catch (Exception e) {
            e.printStackTrace();
            throw new InvalidUserException();
        }
    }

    @Override
    public LightReference toReference() {
        return new BasicReference(this.getUserId(), this.getUsername());
    }

    public static class FriendsResponse {
        public static class FriendData {
            public int UserId;
            public String Username;
        }
        public FriendData[] Friends;
    }

    @Override
    public List<LightReference> getBestFriends() throws Exception {
        List<LightReference> references = new ArrayList<>();
        String url = "https://www.roblox" +
                ".com/friends/json?userId=" + getUserId() +
        "&currentPage=0&pageSize=1000&imgWidth=110&imgHeight=110&imgFormat=jpeg&friendsType" +
                "=BestFriends";
        FriendsResponse response = new Gson().fromJson(Jsoup.connect(url).ignoreContentType(true)
                .get().body().text(), FriendsResponse.class);
        for (FriendsResponse.FriendData friend : response.Friends) {
            references.add(new BasicReference(friend.UserId, friend.Username));
        }
        return references;
    }

    public static class BadgeResponse {
        public static class BadgeData {
            public String Name;
        }
        public BadgeData[] RobloxBadges;
    }

    @Override
    public List<String> getRobloxBadges() throws Exception{
        List<String> badges = new ArrayList<>();
        String url = "https://www.roblox" +
                ".com/badges/roblox?userId=261&imgWidth=110&imgHeight=110&imgFormat=png";
        BadgeResponse response = new Gson().fromJson(Jsoup.connect(url).ignoreContentType(true)
                .get().body().text(), BadgeResponse.class);
        for (BadgeResponse.BadgeData robloxBadge : response.RobloxBadges) {
            badges.add(robloxBadge.Name);
        }
        return badges;
    }

    @Override
    public List<String> getPastUsernames() throws Exception {
        List<String> names = new ArrayList<>();
        Element rootNode = document.getElementsByClass("profile-name-history").first();
        if (rootNode != null) {
            Element pastNamesElement = rootNode.getElementsByClass("tooltip-pastnames").first();
            String pastNamesUnparsed = pastNamesElement.attr("title");
            String[] splitted = pastNamesUnparsed.split(",");
            for (String s : splitted) {
                names.add(s.trim());
            }
        }
        return names;
    }

    @Override
    public boolean isInGroup(Group groupId) throws Exception{
        String url = "https://www.roblox.com/Game/LuaWebService/HandleSocialRequest" +
                ".ashx?method=IsInGroup&playerid=" + this.getUserId() + "&groupid=" + groupId.getId();
        return Boolean.parseBoolean(Jsoup.connect(url).ignoreContentType(true).get().body
                ().text());
    }

    @Override
    public String getProfileUrl() {
        return "https://www.roblox.com/users/" + this.getUserId() + "/profile";
    }

    public static class GroupsResponse {
        public static class GroupData {
            public int Id;
        }
        public GroupData[] Groups;
    }

    public static class CachedResponse {
        public CachedGroupData[] Groups;
    }

    @Override
    public Group[] getGroups() throws Exception {
        String url = "https://www.roblox.com/users/profile/playergroups-json?userId=" + this
                .getUserId();
        GroupsResponse response = new Gson().fromJson(Jsoup.connect(url).ignoreContentType(true)
                .get().body().text(), GroupsResponse.class);
        List<Group> groups = new ArrayList<>();
        for (GroupsResponse.GroupData group : response.Groups) {
            groups.add(Roblox.get(group.Id));
        }
        return groups.toArray(new Group[groups.size()]);
    }

    @Override
    public CachedGroupData[] getCachedGroupData() throws Exception {
        String url = "https://www.roblox.com/users/profile/playergroups-json?userId=" + this
                .getUserId();
        CachedResponse cachedResponse = new Gson().fromJson(Jsoup.connect(url).ignoreContentType
                (true).get().body().text(), CachedResponse.class);
        return cachedResponse.Groups;
    }

    @Override
    public Presence getPresence(AuthenticationInfo authenticationInfo) throws Exception  {
        return Presence.getPresenceForUsers(Arrays.asList(this), authenticationInfo).get(this);
    }

    public static class BadgesResponse {
        public static class BadgesData {
            public String nextPageCursor;
            public static class PlayerBadgeData {
                public static class ItemData {
                    public int AssetId;
                    public String Name;
                }
                public ItemData Item;
            }
            public PlayerBadgeData[] Items;
        }
        public BadgesData Data;
    }

    private List<Badge> recursiveBadgeRetrieve(BadgesResponse response) throws Exception {
        String url = "https://www.roblox" +
                ".com/users/inventory/list-json?assetTypeId=21&cursor=" + response
                .Data.nextPageCursor + "&itemsPerPage=100" +
                "&pageNumber=1&sortOrder=Desc&userId=" + this.getUserId();
        BadgesResponse response1 = new Gson().fromJson(Jsoup.connect(url).ignoreContentType(true)
                .get().body().text(), BadgesResponse.class);
        List<Badge> badges = new ArrayList<>();
        for (BadgesResponse.BadgesData.PlayerBadgeData item : response1.Data.Items) {
            badges.add(new Badge(item.Item.AssetId, item.Item.Name));
        }
        if (response1.Data.nextPageCursor != null) {
            badges.addAll(recursiveBadgeRetrieve(response1));
        }
        return badges;
    }

    public static class AssetDataData {
        public static class Data {
            public String nextPageCursor;
        }
        public Data Data;
    }

    private List<String> getAssetRecursively(int assetId, String cursor) throws Exception {
        String url = "https://www.roblox" +
                ".com/users/inventory/list-json?assetTypeId=" + assetId + "&cursor=" + cursor +
                "&itemsPerPage=100" +
                "&pageNumber=1&sortOrder=Desc&userId=" + this.getUserId();
        String string = Jsoup.connect(url).ignoreContentType(true)
                .get().body().text();
        AssetDataData response1 = new Gson().fromJson(string, AssetDataData.class);
        List<String> pages = new ArrayList<>();
        pages.add(string);
        if (response1.Data.nextPageCursor != null) {
            pages.addAll(getAssetRecursively(assetId, response1.Data.nextPageCursor));
        }
        return pages;
    }

    private List<String> getAssetInventory(int assetId) throws Exception {
        List<String> strings = new ArrayList<>();
        String url = "https://www.roblox" +
                ".com/users/inventory/list-json?assetTypeId=" + assetId + "&cursor="  +
                "&itemsPerPage=100" +
                "&pageNumber=1&sortOrder=Desc&userId=" + this.getUserId();
        String string = Jsoup.connect(url).ignoreContentType(true)
                .get().body().text();
        AssetDataData response1 = new Gson().fromJson(string, AssetDataData.class);
        strings.add(string);
        if (response1.Data.nextPageCursor != null) {
            strings.addAll(getAssetRecursively(assetId, response1.Data.nextPageCursor));
        }
        return strings;
    }

    @Override
    public Badge[] getBadges() throws Exception {
        List<Badge> badges = new ArrayList<>();
        String url = "https://www.roblox" +
                ".com/users/inventory/list-json?assetTypeId=21&cursor=&itemsPerPage=100" +
                "&pageNumber=1&sortOrder=Desc&userId=" + this.getUserId();
        BadgesResponse response = new Gson().fromJson(Jsoup.connect(url).ignoreContentType(true)
                .get().body().text(), BadgesResponse.class);
        for (BadgesResponse.BadgesData.PlayerBadgeData item : response.Data.Items) {
            badges.add(new Badge(item.Item.AssetId, item.Item.Name));
        }
        // next page
        if (response.Data.nextPageCursor != null) {
            badges.addAll(recursiveBadgeRetrieve(response));
        }
        return badges.toArray(new Badge[badges.size()]);
    }

    public static class PlacesResponse {
        public static class GameData {
            public String Description;
            public String Name;
            public int Plays;
            public int PlaceID;
        }
        public GameData[] Games;
    }

    @Override
    public Place[] getPlaces() throws Exception {
        PlacesResponse response = new Gson().fromJson(Jsoup.connect("https://www.roblox" +
                ".com/users/profile/playergames-json?userId=" + getUserId()).ignoreContentType
                (true).get().body().text(), PlacesResponse.class);
        Place[] places = new Place[response.Games.length];
        for (int i = 0; i < response.Games.length; i++) {
            PlacesResponse.GameData data = response.Games[i];
            places[i] = new BasicPlace(data.PlaceID, data.Plays, data.Name, data.Description);
        }
        return places;
    }

    @Override
    public Asset[] getShirts() throws Exception {
        return BasicAsset.getAssetsToArray(getAssetInventory(11));
    }

    @Override
    public Asset[] getPants() throws Exception {
        return BasicAsset.getAssetsToArray(getAssetInventory(12));
    }

    @Override
    public Asset[] getTshirts() throws Exception {
        return BasicAsset.getAssetsToArray(getAssetInventory(2));
    }

    @Override
    public int getJoinTimeInDays() throws Exception {
        Element rootNode = document.getElementsByClass("profile-stats-container").first();
        Elements stats = rootNode.getElementsByClass("profile-stat");
        for (Element stat : stats) {
            String name = stat.getElementsByClass("text-label").first().text();
            if (name.equalsIgnoreCase("Join Date")) {
                String date = stat.getElementsByClass("text-lead").first().text();
                SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyyy");
                Date currentDate = new Date();
                Date newDate = format.parse(date);
                long diff = currentDate.getTime() - newDate.getTime();
                return Math.toIntExact(TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS));
            }
        }
        return 0;
    }

    @Override
    public String getDescription() throws Exception {
        return document.getElementsByClass("profile-about-content-text").first().text();
    }

    @Override
    public ClubType getClub() throws Exception {
        Element root = document.getElementsByClass("profile-header-top").first();
        Element topPart = root.getElementsByClass("header-title").first();
        Elements bc = topPart.getElementsByTag("span");
        if (bc.size() == 0) {
            return ClubType.NONE;
        }
        String bcType = bc.first().className();
        if (bcType.equalsIgnoreCase("icon-bc")) {
            return ClubType.CLASSIC;
        }
        if (bcType.equalsIgnoreCase("icon-tbc")) {
            return ClubType.TURBO;
        }
        return ClubType.OUTRAGEOUS;
    }

    @Override
    public String getProfileImageUrl() throws Exception {
        Element rootNode = document.getElementsByClass("thumbnail-span").first();
        Element img = rootNode.getElementsByTag("img").first();
        return img.attr("src");
    }

    public static void main(String... args) throws Exception {
        BasicRobloxian robloxian = new BasicRobloxian("TGSCommander");
        System.out.println(Arrays.toString(robloxian.getCachedGroupData()));
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof BasicRobloxian)) {
            return false;
        }
        BasicRobloxian robloxian = (BasicRobloxian) obj;
        return robloxian.getUserId() == this.getUserId();
    }

}
