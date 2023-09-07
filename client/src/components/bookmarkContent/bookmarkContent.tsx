import axios from "axios";
import { useEffect, useState } from "react";
import { useForm } from "react-hook-form";
import { styled } from "styled-components";
import { COLOR } from "../../constants/color";
import { FONT_SIZE } from "../../constants/font";
import Button from "../common/Button";
//dto 정해지면 추가

type bookmark = {
  wishId: number;
  creadtedAt: string;
  productId: string;
};

type checkInputType = {
  selectAll: boolean;
  checkboxes: boolean[];
  index: number;
};

const BookmarkContentContainer = styled.form`
  padding: 2rem;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: stretch;
  width: calc(100% - 14rem);
  .checkbox {
    width: 18px;
    height: 18px;
  }
  .topContainer {
    display: flex;
    flex-direction: row;
    justify-content: space-between;
    align-items: center;
    padding: 1.25rem 1rem;
    border-bottom: 3px solid ${COLOR.darkText};
    .menuTitle {
      font-size: ${FONT_SIZE.font_32};
      font-weight: bold;
    }
    .selectButtonContainer {
      display: flex;
      flex-direction: row;
      justify-content: flex-end;
      align-items: center;
      gap: 1rem;
      font-size: ${FONT_SIZE.font_16};
      color: ${COLOR.mediumText};
    }
  }
  .bookmarkListContainer {
    display: flex;
    flex-direction: column;
    justify-content: flex-start;
    align-items: stretch;
    .bookmarkContainer {
      display: flex;
      flex-direction: row;
      justify-content: space-between;
      align-items: center;
      border-bottom: 1px solid ${COLOR.border};
      padding: 1rem 0;
      .leftSection {
        display: flex;
        flex-direction: row;
        justify-content: flex-start;
        align-items: center;
        gap: 1rem;
        .infoContainer {
          display: flex;
          flex-direction: column;
          justify-content: center;
          align-items: flex-start;
          gap: 0.625rem;
          font-size: ${FONT_SIZE.font_16};
          color: ${COLOR.mediumText};
          .postTitle {
            color: ${COLOR.darkText};
            font-size: ${FONT_SIZE.font_20};
            font-weight: bold;
          }
        }
      }
      .rightSection {
        display: flex;
        flex-direction: row;
        justify-content: flex-end;
        align-items: center;
        gap: 1rem;
        .priceContainer {
          display: flex;
          flex-direction: column;
          justify-content: flex-end;
          align-items: center;
          gap: 0.625rem;
          font-size: ${FONT_SIZE.font_16};
          color: ${COLOR.mediumText};
          .priceLabel {
            display: flex;
            flex-direction: row;
            justify-content: flex-end;
            align-items: center;
            gap: 0.5rem;
          }
          .price {
            color: ${COLOR.darkText};
            font-weight: bold;
          }
        }
      }
    }
  }
  .pagenation {
    padding: 1.5rem 0;
  }
`;

const BookmarkContent = (): JSX.Element => {
  const { register, handleSubmit, watch, setValue } = useForm<checkInputType>();
  const checkboxes = watch("checkboxes", []);
  const selectAll = watch("selectAll", false);
  console.log(selectAll, checkboxes);
  const handleSelectAll = (checked: boolean) => {
    setValue("selectAll", checked);
    setValue("checkboxes", Array(bookmarklist.length + 1).fill(checked));
  };
  const handleCheckBox = (index: number, checked: boolean) => {
    const checkedBoxes = [...checkboxes];
    checkedBoxes[index] = checked;
    const lowerCheckbox = checkedBoxes.slice();
    lowerCheckbox.shift();
    const checkedAll = lowerCheckbox.every(Boolean);
    setValue("selectAll", checkedAll);
    setValue("checkboxes", checkedBoxes);
  };
  const sendBookmarkList = (data: checkInputType) => {
    console.log(data);
  };
  const [bookmarklist, setBookmarklist] = useState<bookmark[]>([]);
  const accessToken = localStorage.getItem("accessToken");
  const getData = async () => {
    try {
      const res = await axios.get(`${process.env.REACT_APP_API_URL}/members/wishes`, {
        headers: {
          Authorization: accessToken,
        },
      });
      setBookmarklist(res.data);
    } catch (error) {
      //토큰 만료시 대응하는 함수
      if (axios.isAxiosError(error)) {
        if (error.response?.status === 401) {
        }
      }
    }
  };
  const cancleBookmark = async (wishId: number) => {
    const res = await axios.delete(`${process.env.REACT_APP_API_URL}/wishes/${wishId}`, {
      headers: {
        Authorization: accessToken,
      },
    });
    if (res.status === 200) {
      getData();
      setValue(
        "checkboxes",
        checkboxes.filter((el, idx) => idx !== wishId),
      );
    }
  };
  useEffect(() => {
    getData();
    setValue("checkboxes", Array(bookmarklist.length + 1).fill(false));
  }, []);
  return (
    <BookmarkContentContainer onSubmit={handleSubmit(sendBookmarkList)}>
      <div className="topContainer">
        <p className="menuTitle">찜 목록</p>
        <div className="selectButtonContainer">
          <input
            type="checkbox"
            className="checkbox"
            id="selectAll"
            {...register("selectAll")}
            onChange={(e) => handleSelectAll(e.target.checked)}
          ></input>
          <p className="optionName">전체 선택</p>
          <Button type="submit" $text="선택 취소" $design="yellow" />
        </div>
      </div>
      <div className="bookmarkListContainer">
        {bookmarklist &&
          bookmarklist.map((el, index: number) => (
            <div className="bookmarkContainer" key={el.productId}>
              <div className="leftSection">
                <input
                  type="checkbox"
                  className="checkbox"
                  {...register(`checkboxes.${el.wishId}`)}
                  onChange={(e) => handleCheckBox(index + 1, e.currentTarget.checked)}
                  key={el.productId}
                ></input>
                <img></img>
                <div className="infoContainer">
                  <div className="postTitle">글제목</div>
                  <div>{`남은 시간 `}</div>
                </div>
              </div>
              <div className="rightSection">
                <div className="priceContainer">
                  <div className="priceLabel">
                    {`현재 입찰가`}
                    <span className="price">{` 원`}</span>
                  </div>
                  <div className="priceLabel">
                    {`즉시 구매가`}
                    <span className="price">{` 원`}</span>
                  </div>
                </div>
                <Button
                  type="button"
                  $text="찜 취소"
                  $design="yellow"
                  onClick={() => cancleBookmark(el.wishId)}
                />
              </div>
            </div>
          ))}
      </div>
      <div className="pagenation">페이지네이션</div>
    </BookmarkContentContainer>
  );
};

export default BookmarkContent;