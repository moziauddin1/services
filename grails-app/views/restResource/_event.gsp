<span class="event-gsp"><tt><g:if test="${event}"><st:preferedLink target="${event}"><span style='font-size:87%;'>${event.timeStamp.format('YYYY-MM-dd HH:mm') ?: ifnull ?: '-'}</span></st:preferedLink></g:if><g:else>${ifnull ?: '-'}</g:else></tt></span>