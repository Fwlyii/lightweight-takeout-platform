package elm_bk.service;

import elm_bk.dto.FoodCreateDTO;
import elm_bk.dto.FoodDTO;
import elm_bk.dto.FoodUpdateDTO;
import elm_bk.vo.FoodItemVO;
import elm_bk.vo.FoodVO;

import java.util.List;

public interface FoodService {

    List<FoodVO> getFoodList(Integer business,Integer order);

    FoodVO getFoodById(Long id);

    FoodVO addFood(FoodDTO food);

    FoodVO updateFood(FoodDTO foodDTO,Long id);


    List<FoodItemVO> getFoodItemList(Long businessId,Integer shelveStatus);

    Long addFoodItem(FoodCreateDTO foodCreateDTO);

    Long setFoodStatus(Long foodId,Integer shelveStatus);

    Long modifyFoodMessage(FoodUpdateDTO foodUpdateDTO);
    Long updateStock(FoodUpdateDTO foodUpdateDTO);

    Long deleteFood(Long foodId);


}
