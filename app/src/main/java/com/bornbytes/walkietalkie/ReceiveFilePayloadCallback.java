package com.bornbytes.walkietalkie;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.widget.Toast;

import androidx.collection.SimpleArrayMap;

import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class ReceiveFilePayloadCallback extends PayloadCallback {
    private final Context context;
    private final SimpleArrayMap<Long, Payload> incomingFilePayloads = new SimpleArrayMap<>();
    private final SimpleArrayMap<Long, Payload> completedFilePayloads = new SimpleArrayMap<>();
    private final SimpleArrayMap<Long, String> filePayloadFilenames = new SimpleArrayMap<>();

    public ReceiveFilePayloadCallback(Context context) {
        this.context = context;
    }

    @Override
    public void onPayloadReceived(String endpointId, Payload payload) {
        if (payload.getType() == Payload.Type.BYTES) {
            String payloadFilenameMessage = new String(payload.asBytes(), StandardCharsets.UTF_8);
            long payloadId = addPayloadFilename(payloadFilenameMessage);
            processFilePayload(payloadId);
        } else if (payload.getType() == Payload.Type.FILE) {
            // Add this to our tracking map, so that we can retrieve the payload later.
            incomingFilePayloads.put(payload.getId(), payload);
        }
    }

    /**
     * Extracts the payloadId and filename from the message and stores it in the
     * filePayloadFilenames map. The format is payloadId:filename.
     */
    private long addPayloadFilename(String payloadFilenameMessage) {
        String[] parts = payloadFilenameMessage.split(":");
        long payloadId = Long.parseLong(parts[0]);
        String filename = parts[1];
        filePayloadFilenames.put(payloadId, filename);
        return payloadId;
    }

    private void processFilePayload(long payloadId) {
        // BYTES and FILE could be received in any order, so we call when either the BYTES or the FILE
        // payload is completely received. The file payload is considered complete only when both have
        // been received.
        Payload filePayload = completedFilePayloads.get(payloadId);
        String filename = filePayloadFilenames.get(payloadId);
        if (filePayload != null && filename != null) {
            completedFilePayloads.remove(payloadId);
            filePayloadFilenames.remove(payloadId);

            // Get the received file (which will be in the Downloads folder)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Because of https://developer.android.com/preview/privacy/scoped-storage, we are not
                // allowed to access filepaths from another process directly. Instead, we must open the
                // uri using our ContentResolver.
                Uri uri = Uri.parse(filePayload.asFile().toString());
                try {
                    // Copy the file to a new location.
                    InputStream in = context.getContentResolver().openInputStream(uri);
                    copyStream(in, new FileOutputStream(new File(context.getCacheDir(), filename)));
                } catch (IOException e) {
                    // Log the error.
                } finally {
                    // Delete the original file.
                    context.getContentResolver().delete(uri, null, null);
                }
            } else {
                File payloadFile = filePayload.asFile().asJavaFile();

                payloadFile.renameTo(new File(payloadFile.getParentFile(), filename));
            }

            Toast.makeText(context, "File Received Successfully..", Toast.LENGTH_SHORT).show();

            ((Activity) context).onBackPressed();
        }
    }

    @Override
    public void onPayloadTransferUpdate(String endpointId, PayloadTransferUpdate update) {
        if (update.getStatus() == PayloadTransferUpdate.Status.SUCCESS) {
            long payloadId = update.getPayloadId();
            Payload payload = incomingFilePayloads.remove(payloadId);
            if (payload != null) {
                completedFilePayloads.put(payloadId, payload);
                if (payload.getType() == Payload.Type.FILE) {
                    processFilePayload(payloadId);
                }
            }
        }
    }

    /**
     * Copies a stream from one location to another.
     */
    private static void copyStream(InputStream in, OutputStream out) throws IOException {
        try {
            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            out.flush();
        } finally {
            in.close();
            out.close();
        }
    }
}