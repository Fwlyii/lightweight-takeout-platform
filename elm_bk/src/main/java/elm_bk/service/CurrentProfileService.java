package elm_bk.service;

import elm_bk.mapper.PersonMapper;
import elm_bk.vo.PersonVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CurrentProfileService {
    private final CurrentUserService current;
    private final PersonMapper people;

    @Transactional(readOnly = true)
    public PersonVO get() {
        var account = current.requireUser();
        return ProfileViewFactory.from(account, people.getPersonByUserId(account.getId()));
    }
}
