package com.sissi.utils.lifecycle;

import androidx.lifecycle.LifecycleOwner;

public interface ILifecycleOwner {
    default LifecycleOwner getLifecycleOwner(){return null;}
}
