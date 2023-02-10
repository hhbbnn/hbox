package com.github.tvbox.osc.ui.dialog;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.github.tvbox.osc.R;
import com.github.tvbox.osc.event.RefreshEvent;
import com.github.tvbox.osc.server.ControlManager;
import com.github.tvbox.osc.ui.activity.HomeActivity;
import com.github.tvbox.osc.ui.tv.QRCodeGen;
import com.hjq.permissions.OnPermissionCallback;
import com.hjq.permissions.Permission;
import com.hjq.permissions.XXPermissions;

import org.apache.commons.lang3.StringUtils;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import me.jessyan.autosize.utils.AutoSizeUtils;

/**
 * 描述
 *
 * @author pj567
 * @since 2020/12/27
 */
public class SourceUploadDialog extends BaseDialog {
    private final ImageView ivQRCode;
    private final TextView tvAddress;
    private final EditText inputSourceUrl;

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void refresh(RefreshEvent event) {
        if (event.type == RefreshEvent.TYPE_SOURCE_UPLOAD) {
            inputSourceUrl.setText((String)event.obj);
        }
    }

    public SourceUploadDialog(@NonNull @NotNull Context context) {
        super(context);
        setContentView(R.layout.dialog_source_upload);
        setCanceledOnTouchOutside(true);
        ivQRCode = findViewById(R.id.sourceIvQRCode);
        tvAddress = findViewById(R.id.sourceTvAddress);
        inputSourceUrl = findViewById(R.id.inputSourceUrl);
        String appSource = HomeActivity.getRes().getString(R.string.app_source);
        if(StringUtils.isNotEmpty(appSource)) {
            findViewById(R.id.sourceResetSubmit).setVisibility(View.VISIBLE);
        }

        findViewById(R.id.sourceAddSubmit).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String newApiUrl = inputSourceUrl.getText().toString().trim();
                if (!newApiUrl.isEmpty()) {
                    listener.onAdd(newApiUrl);
                    dismiss();
                }
            }
        });

        findViewById(R.id.sourceReplaceSubmit).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String newApiUrl = inputSourceUrl.getText().toString().trim();
                if (!newApiUrl.isEmpty()) {
                    listener.onReplace(newApiUrl);
                    dismiss();
                }
            }
        });

        findViewById(R.id.sourceResetSubmit).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onReset();
                dismiss();
            }
        });

        findViewById(R.id.sourceStoragePermission).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (XXPermissions.isGranted(getContext(), Permission.Group.STORAGE)) {
                    Toast.makeText(getContext(), "已获得存储权限", Toast.LENGTH_SHORT).show();
                } else {
                    XXPermissions.with(getContext())
                            .permission(Permission.Group.STORAGE)
                            .request(new OnPermissionCallback() {
                                @Override
                                public void onGranted(List<String> permissions, boolean all) {
                                    if (all) {
                                        Toast.makeText(getContext(), "已获得存储权限", Toast.LENGTH_SHORT).show();
                                    }
                                }

                                @Override
                                public void onDenied(List<String> permissions, boolean never) {
                                    if (never) {
                                        Toast.makeText(getContext(), "获取存储权限失败,请在系统设置中开启", Toast.LENGTH_SHORT).show();
                                        XXPermissions.startPermissionActivity((Activity) getContext(), permissions);
                                    } else {
                                        Toast.makeText(getContext(), "获取存储权限失败", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                }
            }
        });
        refreshQRCode();
    }

    private void refreshQRCode() {
        String address = ControlManager.get().getAddress(false);
        tvAddress.setText(String.format("手机/电脑扫描上方二维码或者直接浏览器访问地址\n%s", address));
        ivQRCode.setImageBitmap(QRCodeGen.generateBitmap(address, AutoSizeUtils.mm2px(getContext(), 300), AutoSizeUtils.mm2px(getContext(), 300)));
    }

    public void setOnListener(OnListener listener) {
        this.listener = listener;
    }

    OnListener listener = null;

    public interface OnListener {
        void onAdd(String api);
        void onReplace(String api);
        void onReset();
    }
}