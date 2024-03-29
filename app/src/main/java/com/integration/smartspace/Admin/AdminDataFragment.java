package com.integration.smartspace.Admin;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.ShareCompat;
import android.support.v4.content.FileProvider;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.integration.smartspace.Environment.Environment;
import com.integration.smartspace.R;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import static android.app.Activity.RESULT_OK;

/**
 * Created by liadkh on 5/22/19.
 */
public class AdminDataFragment extends Fragment implements Environment {


    private final int REQUEST_CODE_READ = 102;

    private static final int PICK_FILE_REQUSET = 1;

    final static String TYPE = "type";

    private ProgressDialog mProgressDialog;
    private FuncTypeEnum mFuncTypeEnum;
    private DataTypeEnum mExportDataTypeEnum;
    private String mSmartspace, mMail, mPath;
    private String mBaseURL;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_admin_data, container, false);
        mProgressDialog = new ProgressDialog(getContext(), R.style.ProgressDialogTheme);
        mProgressDialog.setTitle(R.string.please_wait);
        mProgressDialog.setCancelable(false);
        Bundle bundle = this.getArguments();
        if (bundle != null) {
            mFuncTypeEnum = (FuncTypeEnum) bundle.getSerializable(TYPE);
            mSmartspace = bundle.getString(AdminActivity.SMARTSPACE_KEY);
            mMail = bundle.getString(AdminActivity.MAIL_KEY);
            mBaseURL = bundle.getString(BASE_URL);
        }
        view.findViewById(R.id.users_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                play(DataTypeEnum.USERS);
            }
        });
        view.findViewById(R.id.elements_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                play(DataTypeEnum.ELEMENTS);
            }
        });
        view.findViewById(R.id.actions_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                play(DataTypeEnum.ACTIONS);
            }
        });
        return view;
    }

    private void importData(final Uri filePath) {
        mProgressDialog.setMessage(getString(R.string.import_data));
        mProgressDialog.show();

        new Thread(new Runnable() {
            @Override
            public void run() {
                String url = mBaseURL + mPath + "/" + mSmartspace + "/" + mMail;
                RequestQueue queue = Volley.newRequestQueue(getContext());


                StringRequest postRequest = new StringRequest(Request.Method.POST, url,
                        new Response.Listener<String>() {
                            @Override
                            public void onResponse(String response) {
                                getActivity().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(getContext(), R.string.import_succeed, Toast.LENGTH_LONG).show();
                                        mProgressDialog.cancel();
                                    }
                                });
                            }
                        },
                        new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(final VolleyError error) {
                                // error
                                getActivity().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(getContext(), R.string.again_later_and_recheck, Toast.LENGTH_LONG).show();
                                        mProgressDialog.cancel();
                                    }
                                });
                            }
                        }
                ) {
                    @Override
                    public byte[] getBody() throws AuthFailureError {
                        try {
                            byte[] a = readTextFromUri(filePath).getBytes();
                            return a;
                        } catch (Exception e) {
                            throw new AuthFailureError(e.getMessage());
                        }
                    }

                    @Override
                    public String getBodyContentType() {
                        return "application/json";
                    }
                };
                queue.add(postRequest);
            }
        }).start();
    }

    private void exportData() {
        mProgressDialog.setMessage(getString(R.string.export_data));
        mProgressDialog.show();

        new Thread(new Runnable() {
            @Override
            public void run() {
                String url = mBaseURL + mPath + "/" + mSmartspace + "/" + mMail;
                RequestQueue queue = Volley.newRequestQueue(getContext());

                // Request a string response from the provided URL.
                StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                        new Response.Listener<String>() {
                            @Override
                            public void onResponse(String response) {
                                if (response != null && !response.isEmpty()) {
                                    File cacheFolder = new File(getContext().getCacheDir(), "data");
                                    if (!cacheFolder.exists()) {
                                        cacheFolder.mkdirs();
                                    }
                                    File cacheFile = new File(cacheFolder, getContext().getString(R.string.export_data_file) + "-" + mExportDataTypeEnum.name() + "-" + System.currentTimeMillis() + ".txt");
                                    try {
                                        FileWriter writer = new FileWriter(cacheFile);
                                        writer.append(response);
                                        writer.flush();
                                        writer.close();
                                    } catch (IOException e) {
                                        getActivity().runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                Toast.makeText(getContext(), R.string.again_later, Toast.LENGTH_LONG).show();
                                                mProgressDialog.cancel();
                                            }
                                        });
                                        return;
                                    }
                                    Uri uri = FileProvider.getUriForFile(getContext(), "com.integration.smartspace.provider", cacheFile);
                                    Intent intent = ShareCompat.IntentBuilder.from(getActivity())
                                            .setType("text/*")
                                            .setStream(uri)
                                            .createChooserIntent()
                                            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                    getActivity().runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            mProgressDialog.cancel();
                                        }
                                    });
                                    getContext().startActivity(intent);
                                } else {
                                    getActivity().runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            Toast.makeText(getContext(), R.string.empty_data, Toast.LENGTH_LONG).show();
                                            mProgressDialog.cancel();
                                        }
                                    });
                                }
                            }
                        }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(final VolleyError error) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(getContext(), R.string.again_later, Toast.LENGTH_LONG).show();
                                mProgressDialog.cancel();
                            }
                        });
                    }
                });

                // Add the request to the RequestQueue.
                queue.add(stringRequest);
            }
        }).start();

    }

    private String checkType(DataTypeEnum dataTypeEnum) {
        String dataTypeUrl = null;
        switch (dataTypeEnum) {

            case USERS:
                dataTypeUrl = USERS;
                break;
            case ELEMENTS:
                dataTypeUrl = ELEMENTS;

                break;
            case ACTIONS:
                dataTypeUrl = ACTIONS;
                break;
        }
        return dataTypeUrl;
    }


    private void play(DataTypeEnum dataTypeEnum) {
        mExportDataTypeEnum = dataTypeEnum;
        mPath = checkType(dataTypeEnum);
        switch (mFuncTypeEnum) {
            case IMPORT_DATA:
                chooseFile();
                break;
            case EXPORT_DATA:
                exportData();
                break;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {//File has been chosen
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_FILE_REQUSET && resultCode == RESULT_OK && data != null && data.getData() != null) {
            importData(data.getData());
        }
    }


    private String readTextFromUri(Uri uri) throws IOException {
        InputStream inputStream = getContext().getContentResolver().openInputStream(uri);
        BufferedReader reader = new BufferedReader(new InputStreamReader(
                inputStream));
        StringBuilder stringBuilder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            stringBuilder.append(line);
        }
        return stringBuilder.toString();
    }

    private void chooseFile() {

//        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
//        intent.addCategory(Intent.CATEGORY_OPENABLE);
//        intent.setType("text/plain");
//        startActivityForResult(intent, PICK_FILE_REQUSET);


        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            new AlertDialog.Builder(getContext(), R.style.AlertDialogTheme).setTitle(R.string.storage_access)
                    .setMessage(R.string.read_permission)
                    .setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            requestPermissions(
                                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                                    REQUEST_CODE_READ
                            );
                        }
                    }).setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {

                }
            }).show();
        } else {
            Intent intent = new Intent();
            intent.setType("text/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(intent, PICK_FILE_REQUSET);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // If request is cancelled, the result arrays are empty.
        if (requestCode == REQUEST_CODE_READ) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // permission was granted
                chooseFile();
            }
        }
    }
}
