package cgeo.geocaching.settings;

import android.app.Activity;
import android.content.Intent;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import butterknife.ButterKnife;
import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.connector.capability.ICredentials;
import cgeo.geocaching.connector.ec.ECConnector;
import cgeo.geocaching.connector.gc.GCConnector;
import cgeo.geocaching.gcvote.GCVote;
import cgeo.geocaching.network.HtmlImage;
import cgeo.geocaching.settings.AbstractCredentialsAuthorizationActivity.CredentialsAuthParameters;
import cgeo.geocaching.R;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.NonNull;

public class CredentialsPreference extends AbstractClickablePreference {

    private LayoutInflater inflater;

    private static final int NO_KEY = -1;

    private enum CredentialActivityMapping {
        NONE(NO_KEY, null, null, null),
        GEOCACHING(R.string.pref_fakekey_gc_authorization, GCAuthorizationActivity.class, GCAuthorizationActivity.GEOCACHING_CREDENTIAL_AUTH_PARAMS, GCConnector.getInstance()),
        EXTREMCACHING(R.string.pref_fakekey_ec_authorization, ECAuthorizationActivity.class, ECAuthorizationActivity.EXTREMCACHING_CREDENTIAL_AUTH_PARAMS, ECConnector.getInstance()),
        GCVOTE(R.string.pref_fakekey_gcvote_authorization, GCVoteAuthorizationActivity.class, GCVoteAuthorizationActivity.GCVOTE_CREDENTIAL_AUTH_PARAMS, GCVote.getInstance());

        public final int prefKeyId;
        private final Class<?> authActivity;
        private final CredentialsAuthParameters credentialsParams;
        private final ICredentials connector;

        CredentialActivityMapping(final int prefKeyId, @NonNull final Class<?> authActivity, @NonNull final CredentialsAuthParameters credentialsParams, @NonNull final ICredentials connector) {
            this.prefKeyId = prefKeyId;
            this.authActivity = authActivity;
            this.credentialsParams = credentialsParams;
            this.connector = connector;
        }

        public Class<?> getAuthActivity() {
            return authActivity;
        }

        public CredentialsAuthParameters getCredentialsParams() {
            return credentialsParams;
        }

        public ICredentials getConnector() {
            return connector;
        }
    }

    private CredentialActivityMapping getAuthorization() {
        final String prefKey = getKey();
        for (final CredentialActivityMapping auth : CredentialActivityMapping.values()) {
            if (auth.prefKeyId != NO_KEY && prefKey.equals(CgeoApplication.getInstance().getString(auth.prefKeyId))) {
                return auth;
            }
        }
        return CredentialActivityMapping.NONE;
    }

    private final CredentialActivityMapping credentialsMapping;

    public CredentialsPreference(final SettingsActivity settingsActivity, final AttributeSet attrs) {
        super(settingsActivity, attrs);
        this.credentialsMapping = getAuthorization();
        init(settingsActivity);
    }

    public CredentialsPreference(final SettingsActivity settingsActivity, final AttributeSet attrs, final int defStyle) {
        super(settingsActivity, attrs, defStyle);
        this.credentialsMapping = getAuthorization();
        init(settingsActivity);
    }

    private void init(final SettingsActivity settingsActivity) {
        inflater = settingsActivity.getLayoutInflater();
    }

    @Override
    protected OnPreferenceClickListener getOnPreferenceClickListener(final SettingsActivity settingsActivity) {
        settingsActivity.setAuthTitle(credentialsMapping.prefKeyId);
        return new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(final Preference preference) {
                if (credentialsMapping != CredentialActivityMapping.NONE) {
                    final Intent checkIntent = new Intent(preference.getContext(), credentialsMapping.getAuthActivity());
                    credentialsMapping.getCredentialsParams().setCredentialsAuthExtras(checkIntent);
                    settingsActivity.startActivityForResult(checkIntent, credentialsMapping.prefKeyId);
                }
                return false; // no shared preference has to be changed
            }
        };
    }

    @Override
    protected View onCreateView(final ViewGroup parent) {
        super.onCreateView(parent);
        return addInfoIcon(parent);
    }

    /**
     * Display avatar image if present
     */
    private View addInfoIcon(final ViewGroup parent) {
        final View preferenceView = super.onCreateView(parent);

        final String avatarUrl = Settings.getAvatarUrl(credentialsMapping.getConnector());
        if (StringUtils.isEmpty(avatarUrl)) {
            return preferenceView;
        }

        final ImageView iconView = (ImageView) inflater.inflate(R.layout.preference_info_icon, parent, false);
        final HtmlImage imgGetter = new HtmlImage(HtmlImage.SHARED, false, false, false);
        iconView.setImageDrawable(imgGetter.getDrawable(avatarUrl));

        final LinearLayout frame = ButterKnife.findById(preferenceView, android.R.id.widget_frame);
        frame.setVisibility(View.VISIBLE);
        frame.addView(iconView);

        return preferenceView;
    }

    @Override
    protected boolean isAuthorized() {
        return Settings.getCredentials(credentialsMapping.getConnector()).isValid();
    }

    @Override
    protected void revokeAuthorization() {
        Settings.setCredentials(credentialsMapping.getConnector(), Credentials.EMPTY);
        Settings.setAvatarUrl(credentialsMapping.getConnector(), StringUtils.EMPTY);
    }
}
