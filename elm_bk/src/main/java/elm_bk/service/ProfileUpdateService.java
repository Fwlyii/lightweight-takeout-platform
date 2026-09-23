package elm_bk.service;

import elm_bk.dto.ProfileUpdateDTO;
import elm_bk.entity.Person;
import elm_bk.exception.APIException;
import elm_bk.mapper.PersonMapper;
import elm_bk.vo.PersonVO;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProfileUpdateService {
    private final AccountWriteLock writeLock;
    private final PersonMapper people;
    private final CurrentProfileService currentProfile;

    @Transactional
    public PersonVO update(ProfileUpdateDTO request) {
        Long id = writeLock.acquire();
        if (people.countByPhoneExcludingId(request.getPhone(), id) > 0) throw new APIException("手机号已被使用");
        Person person = new Person();
        person.setId(id);
        person.setPhone(request.getPhone());
        person.setEmail(clean(request.getEmail()));
        person.setFirstName(clean(request.getFirstName()));
        person.setLastName(clean(request.getLastName()));
        person.setGender(clean(request.getGender()));
        try { people.updateById(person); }
        catch (DuplicateKeyException ex) { throw new APIException("手机号已被使用"); }
        return currentProfile.get();
    }
    private String clean(String value) { return value == null ? "" : value.trim(); }
}
