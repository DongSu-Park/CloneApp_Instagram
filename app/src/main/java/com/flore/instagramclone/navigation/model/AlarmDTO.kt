package com.flore.instagramclone.navigation.model

data class AlarmDTO (
    var destinationUid : String? = null,
    var userId : String? = null,
    var uid : String? = null,
    var kind : Int? = null, // 알람 상세 타입의 메세지 옵션 번호
    var message : String? = null,
    var timestamp : Long? = null
)

/*
* kind 옵션 번호
* favorite(좋아요) = 0 (DetailViewFragment)
* comment(댓글 등록) = 1 (CommentActivity)
*
* */