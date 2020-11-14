package com.liskovsoft.smartyoutubetv2.common.app.presenters;

import android.annotation.SuppressLint;
import android.content.Context;
import com.liskovsoft.mediaserviceinterfaces.MediaService;
import com.liskovsoft.mediaserviceinterfaces.SignInManager;
import com.liskovsoft.mediaserviceinterfaces.data.Account;
import com.liskovsoft.smartyoutubetv2.common.R;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.ui.OptionItem;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.ui.UiOptionItem;
import com.liskovsoft.smartyoutubetv2.common.utils.RxUtils;
import com.liskovsoft.youtubeapi.service.YouTubeMediaService;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

import java.util.ArrayList;
import java.util.List;

public class AccountSelectionPresenter {
    @SuppressLint("StaticFieldLeak")
    private static AccountSelectionPresenter sInstance;
    private final Context mContext;
    private final SignInManager mSignInManager;
    private Disposable mAccountsAction;
    private Account mSelectedAccount;

    public AccountSelectionPresenter(Context context) {
        mContext = context;
        MediaService service = YouTubeMediaService.instance();
        mSignInManager = service.getSignInManager();
    }

    public static AccountSelectionPresenter instance(Context context) {
        if (sInstance == null) {
            sInstance = new AccountSelectionPresenter(context.getApplicationContext());
        }

        return sInstance;
    }

    public void show() {
        mAccountsAction = mSignInManager.getAccountsObserve()
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::createAndShowDialog);
    }

    public void unhold() {
        RxUtils.disposeActions(mAccountsAction);
        mSelectedAccount = null;
        sInstance = null;
    }

    private void createAndShowDialog(List<Account> accounts) {
        if (accounts == null || accounts.size() <= 1) {
            return;
        }

        AppSettingsPresenter settingsPresenter = AppSettingsPresenter.instance(mContext);
        settingsPresenter.clear();

        createSelectAccountSection(accounts, settingsPresenter);

        settingsPresenter.showDialog(mContext.getString(R.string.settings_accounts), () -> {
            mSignInManager.selectAccount(mSelectedAccount);
            BrowsePresenter.instance(mContext).refresh();

            unhold();
        });
    }

    private void createSelectAccountSection(List<Account> accounts, AppSettingsPresenter settingsPresenter) {
        List<OptionItem> optionItems = new ArrayList<>();

        optionItems.add(UiOptionItem.from(
                mContext.getString(R.string.dialog_account_none), optionItem -> mSelectedAccount = null, true
        ));

        for (Account account : accounts) {
            if (account.isSelected()) {
                mSelectedAccount = account;
            }

            optionItems.add(UiOptionItem.from(
                    formatAccount(account), option -> mSelectedAccount = account, account.isSelected()
            ));
        }

        settingsPresenter.appendRadioCategory(mContext.getString(R.string.dialog_account_list), optionItems);
    }

    private String formatAccount(Account account) {
        String format;

        if (account.getEmail() != null) {
            format = String.format("%s (%s)", account.getName(), account.getEmail());
        } else {
            format = account.getName();
        }

        return format;
    }
}
