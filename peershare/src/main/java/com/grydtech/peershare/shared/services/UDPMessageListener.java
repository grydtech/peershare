package com.grydtech.peershare.shared.services;

import io.reactivex.Observable;

public interface UDPMessageListener {

    Observable<String> listen();
}
