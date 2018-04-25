package org.manlier.analysis.syn;

import org.apache.zookeeper.KeeperException;
import org.manlier.common.zkeepr.SynSignerReceiver;
import org.manlier.common.zkeepr.SynSignerSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class DictStateSynService implements Consumer<String> {

    Logger logger = LoggerFactory.getLogger(DictStateSynService.class.getName());

    private static final DictStateSynService service = new DictStateSynService();

    public static DictStateSynService getInstance() {
        return service;
    }

    public void addDictNeedToSyn(DictSyn dictSyn) {
        logger.info(dictSyn.getClass().getName());
        if (!initialized) return;
        this.synList.put(dictSyn.getClass().getName(), dictSyn);
    }

    private SynSignerSender sender;

    private Map<String, DictSyn> synList = new HashMap<>();

    private boolean initialized = false;

    private DictStateSynService() {
    }

    public void init(String zkHosts, String zkPath) {
        if (!initialized) {
            try {
                this.sender = new SynSignerSender(zkHosts, zkPath);
                new SynSignerReceiver(zkHosts, zkPath, this).process();
            } catch (IOException | InterruptedException | KeeperException e) {
                throw new RuntimeException(e);
            }
            initialized = true;
        }
    }

    public void sendSyncDoneSignal() {
        try {
            sender.sendSynSignal(SynSignal.DICT_SYN_DONE.name());
        } catch (KeeperException | InterruptedException e) {
            logger.error("", e);
        }
    }

    public void runSyncJob() {
        logger.info("Try to synchronize dictionaries");
        logger.info("Syn list size: " + synList.size());
        for (DictSyn dictSyn : synList.values()) {
            logger.info("Syn Type: " + dictSyn.getClass().getName());
            try {
                dictSyn.synDict();
            } catch (IOException e) {
                logger.error("", e);
            }
        }
        logger.info("Synchronize dictionaries done!");
        sendSyncDoneSignal();
    }


    @Override
    public void accept(String s) {
        switch (SynSignal.valueOf(s)) {
            case DICT_SYN_REQ:
                logger.info("Received syn request!");
                CompletableFuture.runAsync(this::runSyncJob);
        }
    }

    public static void main(String[] args) throws InterruptedException {
    }
}
