package com.semibit.ezandroidutils.services;

import android.content.Context;

import com.androidnetworking.error.ANError;
import  com.semibit.ezandroidutils.App;
import  com.semibit.ezandroidutils.Constants;
import  com.semibit.ezandroidutils.R;
import  com.semibit.ezandroidutils.binding.GenericUserViewModel;
import  com.semibit.ezandroidutils.domain.dotpot.models.game.Game;
import  com.semibit.ezandroidutils.interfaces.API;
import  com.semibit.ezandroidutils.interfaces.GenricDataCallback;
import  com.semibit.ezandroidutils.interfaces.GenricObjectCallback;
import  com.semibit.ezandroidutils.interfaces.NetworkRequestCallback;
import  com.semibit.ezandroidutils.interfaces.NetworkService;
import  com.semibit.ezandroidutils.models.ActionItem;
import  com.semibit.ezandroidutils.models.GenricUser;
import  com.semibit.ezandroidutils.ui.BaseActivity;
import  com.semibit.ezandroidutils.utils.ResourceUtils;
import  com.semibit.ezandroidutils.EzUtils;
import com.google.common.base.Strings;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;


public class RestAPI implements API {

    private static RestAPI instance;
    GenricUser user;
    Context ctx;
    NetworkService networkService;

    private RestAPI(Context b) {
        this.ctx = b;
        networkService = AndroidNetworkService.getInstance(b);
    }

    /***
     *
     * @param c Application Context {Context}
     * @return {API}
     */
    public static API getInstance(Context c) {
        if (instance == null)
            instance = new RestAPI(c);
        if (instance.user == null) {
            instance.user = GenericUserViewModel.getInstance().getUser().getValue();
            GenericUserViewModel.getInstance().getUser().observeForever(genricUser -> instance.user = genricUser);
        }
        return instance;
    }

    /***
     *
     * @return {API}
     ***/
    public static RestAPI getInstance() {
        if (instance == null)
            instance = new RestAPI(App.getAppContext());
        return instance;
    }

    public static String getMessageFromANError(ANError job) {
        if (job.getErrorBody() != null) {
            try {
                JSONObject jsonObject = new JSONObject(job.getErrorBody());
                if (jsonObject.getString("message") != null) {
                    return jsonObject.getString("message");
                }
            } catch (Exception ignored) {
            }
            return (job.getErrorBody());
        } else {
            return (job.getErrorDetail());
        }
    }

    public void invalidateCacheWalletAndTxns() {
        if (user != null) {
            CacheService.getInstance().
                    invalidateOne(Constants.API_TRANSACTIONS(user.getId(), ""));
            CacheService.getInstance().invalidateOne(Constants.API_WALLET(user.getId()));
        } else {
            CacheService.getInstance().invalidateAll();
        }
    }

    public void invalidateCacheGames() {
        if (user != null) {
            CacheService.getInstance().invalidateOne(Constants.API_GET_USER_GAMES(user.getId()));
        } else {
            CacheService.getInstance().invalidateAll();
        }
    }

    @Override
    public void getGenricUser(String userId, GenricObjectCallback<GenricUser> cb) {
        cb.onEntity(EzUtils.readUserData());
    }

    public void createTransaction(String url,float amount, GenricObjectCallback<JSONObject> cb){
        invalidateCacheWalletAndTxns();
        JSONObject jop = new JSONObject();
        if (user == null) {
            cb.onError(ctx.getString(R.string.err_pls_login));
            return;
        }
        try {
            jop.put("NAME", user.getName());
            jop.put("EMAIL", user.getEmail());
            jop.put("MOBILE_NO", user.getPhone());
            jop.put("PRODUCT_NAME", BaseActivity.mFirebaseRemoteConfig.getString("PRODUCT_NAME"));
            jop.put("TXN_AMOUNT", amount);

        } catch (Exception e) {
            e.printStackTrace();
        }

        networkService.callPost(url, jop, false, new NetworkRequestCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                if (response.has("payurl")) {
                    cb.onEntity(response);
                } else {
                    cb.onError(ctx.getString(R.string.error_msg));
                }
            }

            @Override
            public void onFail(ANError job) {
                if (job.getErrorBody() != null) {
                    cb.onError(job.getErrorBody());
                } else {
                    cb.onError(job.getErrorDetail());
                }
            }
        });

    }
    @Override
    public void createToken(float amount, GenricObjectCallback<JSONObject> cb) {
        createTransaction(Constants.u(Constants.API_CREATE_TOKEN),amount,cb);
    }

    @Override
    public void createTransaction(float amount, GenricObjectCallback<JSONObject> cb) {
        createTransaction(Constants.u(Constants.API_CREATE_TXN),amount,cb);
    }


    @Override
    public void checkTransaction(String orderId, GenricObjectCallback<JSONObject> cb) {
        JSONObject jop = new JSONObject();

        try {
            jop.put("ORDER_ID", orderId);

        } catch (Exception e) {
            e.printStackTrace();
        }

        networkService.callPost(Constants.u(Constants.API_CHECK_TXN), jop, false, new NetworkRequestCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                if (response.has("status")) {
                    cb.onEntity(response);
                } else {
                    cb.onError(ctx.getString(R.string.error_msg));
                }
            }

            @Override
            public void onFail(ANError job) {
                if (job.getErrorBody() != null) {
                    cb.onError(job.getErrorBody());
                } else {
                    cb.onError(job.getErrorDetail());
                }
            }
        });
    }


    @Override
    public void getWallet(GenricObjectCallback<Wallet> cb) {
        JSONObject jop = new JSONObject();
        if (user == null) {
            cb.onError(ctx.getString(R.string.err_pls_login));
            return;
        }


        networkService.callGet(Constants.API_WALLET(user.getId()), false, new NetworkRequestCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                if (response.has("creditBalance")) {
                    cb.onEntity(new Gson().fromJson(response.toString(), Wallet.class));
                } else {
                    cb.onError(ctx.getString(R.string.error_msg));
                }
            }

            @Override
            public void onFail(ANError job) {
                if (job.getErrorBody() != null) {
                    cb.onError(job.getErrorBody());
                } else {
                    cb.onError(job.getErrorDetail());
                }
            }
        });
    }

    @Override
    public void withdraw(String method, String paytmNo, String upiId, long amount, GenricObjectCallback<String> cb) {

        JSONObject jop = new JSONObject();
        if (!Strings.isNullOrEmpty(paytmNo))
            EzUtils.setKey("paytm", paytmNo, ctx);

        if (!Strings.isNullOrEmpty(upiId))
            EzUtils.setKey("upi", upiId, ctx);

        try {
            jop.put("amount", amount);
            jop.put("paytm", paytmNo);
            jop.put("upi", upiId);
            jop.put("method", method);
        } catch (Exception e) {
            e.printStackTrace();
        }

        networkService.callPost((Constants.API_GET_USER_WITHDRAW(user.getId())), jop, false, new NetworkRequestCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                cb.onEntity(response.optString("message"));
            }

            @Override
            public void onFail(ANError job) {
                if (job.getErrorBody() != null) {
                    cb.onError(job.getErrorBody());
                } else {
                    cb.onError(job.getErrorDetail());
                }
            }
        });

    }

    @Override
    public void getTransactions(String debitOrCredit, GenricObjectCallback<Transaction> cb) {
        JSONObject jop = new JSONObject();
        if (user == null) {
            cb.onError(ctx.getString(R.string.err_pls_login));
            return;
        }

        networkService.callGet(Constants.API_TRANSACTIONS(user.getId(), debitOrCredit), false, new NetworkRequestCallback() {
            @Override
            public void onSuccessString(String response) {

                EzUtils.JSONParser<Transaction> jsonParser = new EzUtils.JSONParser<Transaction>();
                cb.onEntitySet(jsonParser.parseJSONArray(response, Transaction.class));

            }

            @Override
            public void onFail(ANError job) {
                if (job.getErrorBody() != null) {
                    cb.onError(job.getErrorBody());
                } else {
                    cb.onError(job.getErrorDetail());
                }
            }
        });
    }

    @Override
    public void getActionItems(BaseActivity activity, GenricObjectCallback<ActionItem> cb) {

        String jstr = FirebaseRemoteConfig.getInstance().getString("home_menu");
        ArrayList<ActionItem> actionItems = new ArrayList<>();

        if (Strings.isNullOrEmpty(jstr)) {


            ActionItem howToPlay = new ActionItem();
            howToPlay.title = ResourceUtils.getString(R.string.help);
            howToPlay.textAction = ResourceUtils.getString(R.string.view);
            howToPlay.subTitle = ResourceUtils.getString(R.string.how_to_play);
            howToPlay.dateTime = System.currentTimeMillis();
            howToPlay.id = "howto";
            howToPlay.rightTop = "skip";
            howToPlay.accentColorId = EzUtils.colorToHexNoAlpha(ResourceUtils.getColor(R.color.colorTextSuccess));
            howToPlay.actionType = Constants.ACTION_HOW_TO_PLAY;

            actionItems.add(howToPlay);

//        ActionItem actionShowWalletBalance = new ActionItem();
//        actionShowWalletBalance.textAction = ResourceUtils.getString(R.string.add_credits);
//        actionShowWalletBalance.subTitle = ResourceUtils.getString(R.string.help_wallet_bal);
//        actionShowWalletBalance.dateTime = System.currentTimeMillis();
//        actionShowWalletBalance.id = "balance";
//        actionShowWalletBalance.accentColorId = EzUtils.colorToHexNoAlpha(ResourceUtils.getColor(R.color.colorTextSuccess));
//        actionShowWalletBalance.actionType = Constants.ACTION_ADD_CREDITS;
//
//        actionItems.add(actionShowWalletBalance);

            ActionItem actionShowRewards = new ActionItem();
            actionShowRewards.textAction = ResourceUtils.getString(R.string.redeem);
            actionShowRewards.dateTime = System.currentTimeMillis();
            actionShowRewards.id = "redeem";
            actionShowRewards.subTitle = ResourceUtils.getString(R.string.help_award_bal);
            actionShowRewards.accentColorId = EzUtils.colorToHexNoAlpha(ResourceUtils.getColor(R.color.material_teal_500));
            actionShowRewards.actionType = Constants.ACTION_REDEEM_OPTIONS;

            actionItems.add(actionShowRewards);


            ActionItem earnByAds = new ActionItem();
            earnByAds.textAction = ResourceUtils.getString(R.string.get_started);
            earnByAds.dateTime = System.currentTimeMillis();
            earnByAds.id = "earn";
            earnByAds.title = ResourceUtils.getString(R.string.earn);
            earnByAds.rightTop = "skip";
            earnByAds.subTitle = ResourceUtils.getString(R.string.help_earn_bal);
            earnByAds.accentColorId = EzUtils.colorToHexNoAlpha(ResourceUtils.getColor(R.color.material_teal_500));
            earnByAds.actionType = Constants.ACTION_EARN_MONEY;

            actionItems.add(earnByAds);
        } else {
            EzUtils.JSONParser<ActionItem> jsonParser = new EzUtils.JSONParser<ActionItem>();
            actionItems = jsonParser.parseJSONArray(jstr, ActionItem.class);
        }


        actionItems.stream().forEach(actionItem -> actionItem.act = activity);
        cb.onEntitySet(actionItems);

    }

    @Override
    public void getLeaderBoard(GenricObjectCallback<GenricUser> cb) {

        networkService.callGet(Constants.HOST + Constants.API_GET_LEADERBOARD
                , false, new NetworkRequestCallback() {
                    @Override
                    public void onSuccessString(String response) {

                        EzUtils.JSONParser<GenricUser> jsonParser = new EzUtils.JSONParser<GenricUser>();
                        cb.onEntitySet(jsonParser.parseJSONArray(response, GenricUser.class));

                    }

                    @Override
                    public void onFail(ANError job) {
                        cb.onError(getMessageFromANError(job));
                    }
                });
    }


    @Override
    public void redeemReferral(String referralCode, GenricDataCallback cb) {

        JSONObject jop = new JSONObject();
        try {

            jop.put("referralCode", referralCode);

        } catch (Exception e) {
        }

        networkService.callPost(Constants.u(Constants.API_REDEEM_REFERRAL), jop, false, new NetworkRequestCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                cb.onStart(response.optString("message"), 1);
            }

            @Override
            public void onFail(ANError job) {
                cb.onStart(getMessageFromANError(job), -1);
            }
        });


    }

    private ArrayList<Float> parseAmounts(String responseJArray) {
        ArrayList<Float> amts = new ArrayList<>();
        try {
            JSONArray jsonArray = new JSONArray(responseJArray);

            for (int i = 0; i < jsonArray.length(); i++) {
                Float amt = (float) jsonArray.optDouble(i, 0.0d);
                amts.add(amt);
            }
        } catch (Exception e) {
            e.printStackTrace();
            CrashReporter.reportException(e);
        }
        return amts;
    }

    @Override
    public void getGameAmounts(GenricObjectCallback<Float> cb) {
        String amtJar = EzUtils.getKey("amounts", ctx);
        if (EzUtils.isEmpty(amtJar)) {
            networkService.callGet(Constants.u(Constants.API_GET_GAME_AMOUNTS), false, new NetworkRequestCallback() {
                @Override
                public void onSuccessString(String response) {

                    EzUtils.setKey("amounts", response, ctx);
                    cb.onEntitySet(parseAmounts(response));

                }

                @Override
                public void onFail(ANError job) {
                    cb.onError(getMessageFromANError(job));
                }
            });
        } else {
            cb.onEntitySet(parseAmounts(amtJar));
        }
    }


    @Override
    public void getPayAmounts(GenricObjectCallback<Float> cb) {
        String amtJar = EzUtils.getKey("pamounts", ctx);
        if (EzUtils.isEmpty(amtJar)) {
            networkService.callGet(Constants.u(Constants.API_GET_PAY_AMOUNTS), false, new NetworkRequestCallback() {
                @Override
                public void onSuccessString(String response) {

                    EzUtils.setKey("pamounts", response, ctx);
                    cb.onEntitySet(parseAmounts(response));

                }

                @Override
                public void onFail(ANError job) {
                    cb.onError(getMessageFromANError(job));
                }
            });
        } else {
            cb.onEntitySet(parseAmounts(amtJar));
        }
    }

    @Override
    public void getUserGames(int currentGameListSize, GenricObjectCallback<Game> cb) {

        networkService.callGet(Constants.u(Constants.API_GET_USER_GAMES(user.getId())) + "?limit=" + FirebaseRemoteConfig.getInstance().getLong("page_size") + "&offset=" + currentGameListSize
                , false, new NetworkRequestCallback() {
                    @Override
                    public void onSuccessString(String response) {

                        EzUtils.JSONParser<Game> jsonParser = new EzUtils.JSONParser<Game>();
                        cb.onEntitySet(jsonParser.parseJSONArray(response, Game.class));

                    }

                    @Override
                    public void onFail(ANError job) {
                        cb.onError(getMessageFromANError(job));
                    }
                });
    }

    @Override
    public void createGame(Float amount, String player2Id, GenricObjectCallback<Game> cb) {

        invalidateCacheWalletAndTxns();
        invalidateCacheGames();
        JSONObject jop = new JSONObject();
        try {

            jop.put("gameType", amount > 0 ? "paid" : "free");
            jop.put("amount", amount);
            jop.put("player2Id", player2Id);

        } catch (Exception e) {
        }

        networkService.callPost(Constants.u(Constants.API_CREATE_GAME), jop,
                false, new NetworkRequestCallback() {
                    @Override
                    public void onSuccess(JSONObject response) {
                        if(amount > 0)
                            WalletViewModel.getInstance().refresh(null);
                        cb.onEntity(EzUtils.js.fromJson(response.toString(), Game.class));
                    }

                    @Override
                    public void onFail(ANError job) {
                        cb.onError(getMessageFromANError(job));
                    }
                });


    }

    @Override
    public void finishGame(Game game, GenricObjectCallback<Game> cb) {


        JSONObject jop = new JSONObject();
        try {

            jop.put("id", game.getId());
            jop.put("player1Time", game.getPlayer1Time());
            jop.put("player2Time", game.getPlayer2Time());
            jop.put("player1wins", game.getPlayer1wins());
            jop.put("player2wins", game.getPlayer2wins());

        } catch (Exception e) {
        }

        networkService.callPost(Constants.u(Constants.API_FINISH_GAME), jop,
                false, new NetworkRequestCallback() {
                    @Override
                    public void onSuccess(JSONObject response) {
                        Game g = EzUtils.js.fromJson(response.toString(), Game.class);
                        cb.onEntity(g);
                    }

                    @Override
                    public void onFail(ANError job) {
                        cb.onError(getMessageFromANError(job));
                    }
                });
    }


    @Override
    public void getProducts(int currentGameListSize, String contextType, GenricObjectCallback<Product> cb) {

        if (contextType.equals("earn"))
            contextType = "earn";
        else
            contextType = "shop";
        networkService.callGet(Constants.u(Constants.API_GET_PRODUCTS) + "?type=" + contextType + "&limit=" + FirebaseRemoteConfig.getInstance().getLong("page_size") + "&offset=" + currentGameListSize
                , false, new NetworkRequestCallback() {
                    @Override
                    public void onSuccessString(String response) {

                        EzUtils.JSONParser<Product> jsonParser = new EzUtils.JSONParser<Product>();
                        cb.onEntitySet(jsonParser.parseJSONArray(response, Product.class));

                    }

                    @Override
                    public void onFail(ANError job) {
                        cb.onError(getMessageFromANError(job));
                    }
                });
    }

    @Override
    public void getUserProducts(int currentGameListSize, String contextType, GenricObjectCallback<Product> cb) {

        if (contextType.equals("earn"))
            contextType = "earn";
        else
            contextType = "shop";
        networkService.callGet(Constants.u(Constants.API_GET_USER_PRODUCTS(user.getId())) + "?type=" + contextType + "&limit=" + FirebaseRemoteConfig.getInstance().getLong("page_size") + "&offset=" + currentGameListSize
                , false, new NetworkRequestCallback() {
                    @Override
                    public void onSuccessString(String response) {

                        EzUtils.JSONParser<Product> jsonParser = new EzUtils.JSONParser<Product>();
                        cb.onEntitySet(jsonParser.parseJSONArray(response, Product.class));

                    }

                    @Override
                    public void onFail(ANError job) {
                        cb.onError(getMessageFromANError(job));
                    }
                });
    }

    @Override
    public void buyProduct(String productId, GenricObjectCallback<Product> cb) {


        JSONObject jop = new JSONObject();
        try {

            jop.put("id", productId);

        } catch (Exception e) {
        }

        networkService.callPost(Constants.API_BUY_PRODUCT(productId), jop,
                false, new NetworkRequestCallback() {
                    @Override
                    public void onSuccess(JSONObject response) {
                        cb.onEntity(EzUtils.js.fromJson(response.toString(), Product.class));
                    }

                    @Override
                    public void onFail(ANError job) {
                        cb.onError(getMessageFromANError(job));
                    }
                });


    }


    @Override
    public void checkUpdate(int versionCode, GenricObjectCallback<JSONObject> cb) {

        String updUrl = FirebaseRemoteConfig.getInstance().getString("update_check_url");
        networkService.callGet(updUrl, false, new NetworkRequestCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                cb.onEntity(response);
            }

            @Override
            public void onFail(ANError job) {
                cb.onError(getMessageFromANError(job));
            }
        });
    }
}