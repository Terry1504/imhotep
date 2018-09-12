/*
 * Copyright (C) 2018 Indeed Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.indeed.imhotep.shardmasterrpc;
import com.indeed.imhotep.DatasetInfo;
import com.indeed.imhotep.Shard;
import com.indeed.imhotep.ShardInfo;
import com.indeed.imhotep.client.Host;
import com.indeed.imhotep.protobuf.*;
import org.apache.log4j.Logger;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author kenh
 */

public class RequestResponseClient implements ShardMaster {
    private static final Logger LOGGER = Logger.getLogger(RequestResponseClient.class);
    private final Host serverHost;
    private static String currentHostName;
    static {
        try {
            currentHostName = java.net.InetAddress.getLocalHost().getHostName();
        }
        catch (java.net.UnknownHostException ex) {
            currentHostName = "(unknown)";
        }
    }

    public RequestResponseClient(final Host serverHost) {
        this.serverHost = serverHost;
    }

    private ShardMasterResponse receiveResponse(final ShardMasterRequest request, final InputStream is) throws IOException {
        final ShardMasterResponse response = ShardMasterMessageUtil.receiveResponse(is);
        switch (response.getResponseCode()) {
            case OK:
                return response;
            case ERROR:
                throw new IOException("Received error from " + serverHost + " for request " + request + ": " + response.getErrorMessage());
            default:
                throw new IllegalStateException("Received unexpected response code " + response.getResponseCode());
        }
    }

    private List<ShardMasterResponse> sendAndReceive(final ShardMasterRequest request) throws IOException {
        try (Socket socket = new Socket(serverHost.getHostname(), serverHost.getPort())) {
            ShardMasterMessageUtil.sendMessage(request, socket.getOutputStream());
            try (InputStream socketInputStream = socket.getInputStream()) {
                final List<ShardMasterResponse> responses = new ArrayList<>();
                while (true) {
                    try {
                        responses.add(receiveResponse(request, socketInputStream));
                    } catch (final EOFException e) {
                        return responses;
                    }
                }
            }
        }
    }

    @Override
    public List<DatasetInfo> getDatasetMetadata() throws IOException{
        final ShardMasterRequest request = ShardMasterRequest.newBuilder()
                .setRequestType(ShardMasterRequest.RequestType.GET_DATASET_METADATA)
                .setNode(HostAndPort.newBuilder().setHost(currentHostName).build())
                .build();
        final List<ShardMasterResponse> shardMasterResponses = sendAndReceive(request);

        final List<DatasetInfo> toReturn = new ArrayList<>();
        for(ShardMasterResponse response: shardMasterResponses){
            for(DatasetInfoMessage metadata: response.getMetadataList()) {
                toReturn.add(DatasetInfo.fromProto(metadata));
            }
        }
        return toReturn;
    }

    @Override
    public List<Shard> getShardsInTime(final String dataset, final long start, final long end) throws IOException {
        final ShardMasterRequest request = ShardMasterRequest.newBuilder()
                .setRequestType(ShardMasterRequest.RequestType.GET_SHARD_LIST_FOR_TIME)
                .setStartTime(start)
                .setEndTime(end)
                .setDataset(dataset)
                .setNode(HostAndPort.newBuilder().setHost(currentHostName).build())
                .build();
        final List<Shard> toReturn = new ArrayList<>();
        final List<ShardMasterResponse> shardMasterResponses = sendAndReceive(request);
        for(ShardMasterResponse response: shardMasterResponses){
            final List<ShardMessage> shardsInTimeList = response.getShardsInTimeList();
            for(ShardMessage message: shardsInTimeList) {
                Host host = new Host(message.getHost().getHost(), message.getHost().getPort());
                Shard shard = new Shard(message.getShardId(), message.getNumDocs(), message.getVersion(), host, message.getExtension());
                toReturn.add(shard);
            }
        }
        return toReturn;
    }

    @Override
    public Map<String, Collection<ShardInfo>> getShardList() throws IOException {
        final ShardMasterRequest request = ShardMasterRequest.newBuilder()
                .setRequestType(ShardMasterRequest.RequestType.GET_SHARD_LIST)
                .setNode(HostAndPort.newBuilder().setHost(currentHostName).build())
                .build();

        final List<DatasetShardsMessage> datasetMessages = sendAndReceive(request).stream()
                .map(ShardMasterResponse::getAllShardsList)
                .flatMap(List::stream).collect(Collectors.toList());

        final Map<String, Collection<ShardInfo>> toReturn = new HashMap<>();

        for(DatasetShardsMessage message: datasetMessages) {
            toReturn.put(message.getDataset(), message.getShardsList().stream().map(ShardInfo::fromProto).collect(Collectors.toList()));
        }

        return toReturn;
    }

    @Override
    public void refreshFieldsForDataset(String dataset) throws IOException {
        final ShardMasterRequest request = ShardMasterRequest.newBuilder()
                .setRequestType(ShardMasterRequest.RequestType.REFRESH_FIELDS_FOR_DATASET)
                .setNode(HostAndPort.newBuilder().setHost(currentHostName).build())
                .setDatasetToRefresh(dataset).build();
        sendAndReceive(request);
    }
}