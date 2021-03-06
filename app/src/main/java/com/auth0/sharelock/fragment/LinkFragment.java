package com.auth0.sharelock.fragment;


import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.auth0.sharelock.R;
import com.auth0.sharelock.Secret;
import com.auth0.sharelock.event.NewLinkEvent;
import com.auth0.sharelock.event.RequestLinkEvent;
import com.auth0.sharelock.event.RequestNewSecretEvent;
import com.auth0.sharelock.event.SharelockAPIErrorEvent;
import com.auth0.sharelock.widget.ShareEditText;
import com.nispok.snackbar.Snackbar;
import com.nispok.snackbar.SnackbarManager;

import de.greenrobot.event.EventBus;

public class LinkFragment extends Fragment {

    public static final String LINK_FRAGMENT_SECRET_ARGUMENT = "LINK_FRAGMENT_SECRET_ARGUMENT";

    Secret secret;
    EventBus bus;
    Uri link;

    TextView linkText;
    ProgressBar progressBar;
    Button retryButton;
    ImageButton shareButton;
    ImageButton newButton;
    ViewGroup buttons;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Bundle arguments = getArguments();
        if (arguments != null) {
            secret = arguments.getParcelable(LINK_FRAGMENT_SECRET_ARGUMENT);
        }
        bus = EventBus.getDefault();
    }

    @Override
    public void onStart() {
        super.onStart();
        bus.registerSticky(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        bus.unregister(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_link, container, false);
    }

    @Override
    public void onViewCreated(final View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        final EventBus bus = this.bus;

        TextView secretText = (TextView) view.findViewById(R.id.link_secret_text);
        secretText.setText(secret.getSecret());
        ShareEditText shareEditText = (ShareEditText) view.findViewById(R.id.link_share_list);
        shareEditText.setFocusable(false);
        shareEditText.allowDuplicates(false);
        for (String viewer: secret.getAllowedViewers()) {
            shareEditText.addObject(viewer);
        }
        linkText = (TextView) view.findViewById(R.id.link_text);
        progressBar = (ProgressBar) view.findViewById(R.id.link_progress);
        retryButton = (Button) view.findViewById(R.id.link_retry_button);
        shareButton = (ImageButton) view.findViewById(R.id.link_share_button);
        newButton = (ImageButton) view.findViewById(R.id.link_new_button);
        buttons = (ViewGroup) view.findViewById(R.id.link_buttons);
        retryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bus.post(new RequestLinkEvent(secret));
            }
        });
        shareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_TEXT, link.toString());
                sendIntent.setType("text/plain");
                startActivity(Intent.createChooser(sendIntent, getString(R.string.share_link_chooser_title)));
            }
        });
        newButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog dialog = new AlertDialog.Builder(getActivity())
                        .setTitle(R.string.new_link_prompt_title)
                        .setMessage(R.string.new_link_prompt_message)
                        .setCancelable(true)
                        .setPositiveButton(R.string.ok_button, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                bus.post(new RequestNewSecretEvent());
                            }
                        })
                        .setNegativeButton(R.string.cancel_button, null)
                        .create();
                dialog.show();
            }
        });
        ImageView craftedBy = (ImageView) view.findViewById(R.id.crafted_by);
        craftedBy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(getString(R.string.crafted_by_url)));
                startActivity(intent);
            }
        });
    }

    public void onEventMainThread(NewLinkEvent event) {
        progressBar.setVisibility(View.GONE);
        buttons.setVisibility(View.VISIBLE);
        link = event.getLink();
        linkText.setText(link.toString());
    }

    public void onEventMainThread(SharelockAPIErrorEvent event) {
        linkText.setText(R.string.link_generation_failed_message);
        progressBar.setVisibility(View.GONE);
        retryButton.setVisibility(View.VISIBLE);
        buttons.setVisibility(View.GONE);
    }

    public void onEventMainThread(RequestLinkEvent event) {
        retryButton.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);
        linkText.setText(R.string.link_in_progress);
        buttons.setVisibility(View.GONE);
    }
}
