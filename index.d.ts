
import React from 'react';

declare const isQQInstalled: () => Promise<boolean>;
declare const isWeiboInstalled: () => Promise<boolean>;
declare const share: (data: any) => Promise<any>;

export {
    share,
    isQQInstalled,
    isWeiboInstalled,
};

