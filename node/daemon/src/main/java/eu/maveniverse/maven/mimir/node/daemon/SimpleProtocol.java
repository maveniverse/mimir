/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.mimir.node.daemon;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class SimpleProtocol {
    private SimpleProtocol() {}

    // server side processing successful
    public static final String OK = "OK";
    // server side processing error
    public static final String KO = "KO ";

    public static final String CMD_LOCATE = "LOCATE";
    public static final String CMD_TRANSFER = "TRANSFER";
    public static final String CMD_LS_CHECKSUMS = "LS_CHECKSUMS";
    public static final String CMD_STORE_PATH = "STORE_PATH";

    // LOCATE command:
    // Request:
    // - LOCATE
    // - key
    // Response:
    // - OK
    // - map size == 0 => logical failure (not found)
    // - pair=pair
    // or
    // - KO
    // - Message

    // TRANSFER command:
    // Request:
    // - TRANSFER
    // - key
    // - path
    // Response:
    // - OK
    // or
    // - KO
    // - Message

    // LS_CHECKSUMS command:
    // Request:
    // - LS_CHECKSUMS
    // Response:
    // - OK
    // - list size (> 0)
    // - entry
    // or
    // - KO
    // - Message

    // STORE_PATH command;
    // Request:
    // - STORE_PATH
    // - key
    // - path
    // - checksums size > 0
    // - pair=pair
    // Response:
    // - OK
    // or
    // - KO
    // - Message

    public static void writeRspKO(DataOutputStream dos, String errorMessage) throws IOException {
        dos.writeUTF(KO);
        dos.writeUTF(errorMessage);
        dos.flush();
    }

    public static void writeSimpleRspOK(DataOutputStream dos) throws IOException {
        dos.writeUTF(OK);
        dos.flush();
    }

    public static void readSimpleRsp(DataInputStream dis) throws IOException {
        String result = dis.readUTF();
        if (!OK.equals(result)) {
            String errorMessage = dis.readUTF();
            throw new IOException(errorMessage);
        }
    }

    public static Map<String, String> readMap(DataInputStream dis) throws IOException {
        int pairs = dis.readInt();
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < pairs; i++) {
            map.put(dis.readUTF(), dis.readUTF());
        }
        return map;
    }

    // LOCATE

    public static void writeLocateReq(DataOutputStream dos, String key) throws IOException {
        dos.writeUTF(CMD_LOCATE);
        dos.writeUTF(key);
        dos.flush();
    }

    public static void writeLocateRspOK(DataOutputStream dos, Map<String, String> data) throws IOException {
        dos.writeUTF(OK);
        dos.writeInt(data.size());
        for (Map.Entry<String, String> entry : data.entrySet()) {
            dos.writeUTF(entry.getKey());
            dos.writeUTF(entry.getValue());
        }
        dos.flush();
    }

    public static Map<String, String> readLocateRsp(DataInputStream dis) throws IOException {
        String result = dis.readUTF();
        if (OK.equals(result)) {
            return readMap(dis);
        } else {
            String errorMessage = dis.readUTF();
            throw new IOException(errorMessage);
        }
    }

    // TRANSFER

    public static void writeTransferReq(DataOutputStream dos, String key, String path) throws IOException {
        dos.writeUTF(CMD_TRANSFER);
        dos.writeUTF(key);
        dos.writeUTF(path);
        dos.flush();
    }

    public static void writeTransferRspOK(DataOutputStream dos) throws IOException {
        writeSimpleRspOK(dos);
    }

    public static void readTransferRsp(DataInputStream dis) throws IOException {
        readSimpleRsp(dis);
    }

    // LS_CHECKSUMS

    public static void writeLsChecksumsReq(DataOutputStream dos) throws IOException {
        dos.writeUTF(CMD_LS_CHECKSUMS);
        dos.flush();
    }

    public static void writeLsChecksumsRspOK(DataOutputStream dos, List<String> checksums) throws IOException {
        dos.writeUTF(OK);
        dos.writeInt(checksums.size());
        for (String checksum : checksums) {
            dos.writeUTF(checksum);
        }
        dos.flush();
    }

    public static List<String> readLsChecksumsRsp(DataInputStream dis) throws IOException {
        String result = dis.readUTF();
        if (OK.equals(result)) {
            int pairs = dis.readInt();
            ArrayList<String> list = new ArrayList<>(pairs);
            for (int i = 0; i < pairs; i++) {
                list.add(dis.readUTF());
            }
            return list;
        } else {
            String errorMessage = dis.readUTF();
            throw new IOException(errorMessage);
        }
    }

    // STORE_PATH

    public static void writeStorePathReq(DataOutputStream dos, String key, String path, Map<String, String> checksums)
            throws IOException {
        dos.writeUTF(CMD_STORE_PATH);
        dos.writeUTF(key);
        dos.writeUTF(path);
        dos.writeInt(checksums.size());
        for (Map.Entry<String, String> entry : checksums.entrySet()) {
            dos.writeUTF(entry.getKey());
            dos.writeUTF(entry.getValue());
        }
        dos.flush();
    }

    public static void writeStorePathRspOK(DataOutputStream dos) throws IOException {
        writeSimpleRspOK(dos);
    }

    public static void readStorePathRsp(DataInputStream dis) throws IOException {
        readSimpleRsp(dis);
    }
}
